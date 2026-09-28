package io.github.sim2200.agenteval.run;

import static org.assertj.core.api.Assertions.*;

import io.github.sim2200.agenteval.domain.CaseResult;
import io.github.sim2200.agenteval.domain.CheckResult;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.scoring.Scorer;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunSummaryAndDiffTest {

  @Test
  void runSummaryCounts() {
    // 3 cases x repeat 3 with mixed outcomes
    CheckResult checkPass = new CheckResult(0, "contains", true, new Outcome.Pass(), 1);
    CheckResult checkFail = new CheckResult(0, "contains", true, new Outcome.Fail("no"), 1);

    List<CaseResult> results =
        List.of(
            // c1: 2 passes, 1 fail -> STABLE_PASS
            new CaseResult("c1", 0, List.of(checkPass)),
            new CaseResult("c1", 1, List.of(checkPass)),
            new CaseResult("c1", 2, List.of(checkFail)),
            // c2: 1 pass, 2 fails -> FLAKY
            new CaseResult("c2", 0, List.of(checkPass)),
            new CaseResult("c2", 1, List.of(checkFail)),
            new CaseResult("c2", 2, List.of(checkFail)),
            // c3: 0 passes, 3 fails -> STABLE_FAIL
            new CaseResult("c3", 0, List.of(checkFail)),
            new CaseResult("c3", 1, List.of(checkFail)),
            new CaseResult("c3", 2, List.of(checkFail)));

    RunSummary summary = RunSummary.of(results, new Scorer(3), 10L);

    assertThat(summary.cases()).isEqualTo(3);
    assertThat(summary.executions()).isEqualTo(9);
    assertThat(summary.repeat()).isEqualTo(3);
    assertThat(summary.stablePass()).isEqualTo(1);
    assertThat(summary.flaky()).isEqualTo(1);
    assertThat(summary.stableFail()).isEqualTo(1);
    assertThat(summary.passRate()).isCloseTo(0.333, within(0.01));
  }

  @Test
  void runSummaryCheckMetrics() {
    // Construct results with mixed outcomes for precision/recall testing
    CheckResult shouldPassResult = new CheckResult(0, "contains", true, new Outcome.Pass(), 1);
    CheckResult shouldFailResult = new CheckResult(0, "contains", false, new Outcome.Fail("no"), 1);
    CheckResult wrongPassResult = new CheckResult(0, "regex", true, new Outcome.Fail("no"), 1);
    CheckResult wrongFailResult = new CheckResult(0, "regex", false, new Outcome.Pass(), 1);

    List<CaseResult> results =
        List.of(
            new CaseResult("c1", 0, List.of(shouldPassResult)),
            new CaseResult("c2", 0, List.of(shouldFailResult)),
            new CaseResult("c3", 0, List.of(wrongPassResult)),
            new CaseResult("c4", 0, List.of(wrongFailResult)));

    RunSummary summary = RunSummary.of(results, new Scorer(1), 10L);

    // contains: 1 TP (c2), 0 FP, 0 FN, 0 errors
    RunSummary.CheckMetrics containsMetrics = summary.checks().get("contains");
    assertThat(containsMetrics.evaluations()).isEqualTo(2);
    assertThat(containsMetrics.truePositives()).isEqualTo(1);

    // regex: 0 TP, 1 FP (c3), 1 FN (c4), 0 errors
    RunSummary.CheckMetrics regexMetrics = summary.checks().get("regex");
    assertThat(regexMetrics.evaluations()).isEqualTo(2);
    assertThat(regexMetrics.falsePositives()).isEqualTo(1);
    assertThat(regexMetrics.falseNegatives()).isEqualTo(1);
  }

  @Test
  void checkMetricsPrecisionAndRecall() {
    RunSummary.CheckMetrics metrics = new RunSummary.CheckMetrics(10, 3, 2, 5, 0);

    // precision = TP / (TP + FP) = 3 / 5 = 0.6
    assertThat(metrics.precision()).isCloseTo(0.6, within(0.01));

    // recall = TP / (TP + FN) = 3 / 8 = 0.375
    assertThat(metrics.recall()).isCloseTo(0.375, within(0.01));
  }

  @Test
  void checkMetricsDivideByZeroConvention() {
    // No fires at all -> precision = 1.0
    RunSummary.CheckMetrics noFires = new RunSummary.CheckMetrics(5, 0, 0, 2, 0);
    assertThat(noFires.precision()).isEqualTo(1.0);

    // Nothing should have fired -> recall = 1.0
    RunSummary.CheckMetrics noMisses = new RunSummary.CheckMetrics(5, 0, 1, 0, 1);
    assertThat(noMisses.recall()).isEqualTo(1.0);
  }

  @Test
  void runDiff() {
    List<Scorer.CaseScore> baseScores =
        List.of(
            new Scorer.CaseScore("c1", 3, 2, Scorer.Band.STABLE_PASS),
            new Scorer.CaseScore("c2", 3, 1, Scorer.Band.FLAKY),
            new Scorer.CaseScore("c3", 3, 0, Scorer.Band.STABLE_FAIL));

    List<Scorer.CaseScore> otherScores =
        List.of(
            new Scorer.CaseScore("c1", 3, 1, Scorer.Band.FLAKY),
            new Scorer.CaseScore("c2", 3, 2, Scorer.Band.STABLE_PASS),
            new Scorer.CaseScore("c4", 3, 3, Scorer.Band.STABLE_PASS));

    RunDiff diff = RunDiff.of(1L, baseScores, 2L, otherScores);

    assertThat(diff.baseRunId()).isEqualTo(1L);
    assertThat(diff.otherRunId()).isEqualTo(2L);
    assertThat(diff.compared()).isEqualTo(2);
    assertThat(diff.regressed()).hasSize(1).extracting(RunDiff.Flip::caseId).contains("c1");
    assertThat(diff.improved()).hasSize(1).extracting(RunDiff.Flip::caseId).contains("c2");
    assertThat(diff.onlyInBase()).containsExactly("c3");
    assertThat(diff.onlyInOther()).containsExactly("c4");
  }

  @Test
  void runDiffSortedIds() {
    List<Scorer.CaseScore> baseScores =
        List.of(
            new Scorer.CaseScore("z", 3, 2, Scorer.Band.STABLE_PASS),
            new Scorer.CaseScore("a", 3, 0, Scorer.Band.STABLE_FAIL),
            new Scorer.CaseScore("m", 3, 0, Scorer.Band.STABLE_FAIL));

    List<Scorer.CaseScore> otherScores =
        List.of(
            new Scorer.CaseScore("z", 3, 0, Scorer.Band.STABLE_FAIL),
            new Scorer.CaseScore("a", 3, 2, Scorer.Band.STABLE_PASS),
            new Scorer.CaseScore("x", 3, 0, Scorer.Band.STABLE_FAIL));

    RunDiff diff = RunDiff.of(1L, baseScores, 2L, otherScores);

    assertThat(diff.regressed()).extracting(RunDiff.Flip::caseId).containsExactly("z");
    assertThat(diff.improved()).extracting(RunDiff.Flip::caseId).containsExactly("a");
    assertThat(diff.onlyInBase()).containsExactly("m");
    assertThat(diff.onlyInOther()).containsExactly("x");
  }
}
