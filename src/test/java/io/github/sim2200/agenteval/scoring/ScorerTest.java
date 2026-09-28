package io.github.sim2200.agenteval.scoring;

import static org.assertj.core.api.Assertions.*;

import io.github.sim2200.agenteval.domain.CaseResult;
import io.github.sim2200.agenteval.domain.CheckResult;
import io.github.sim2200.agenteval.domain.Outcome;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScorerTest {

  @Test
  void defaultThresholdForRepeat() {
    // 1->1, 2->2, 3->2, 4->3, 5->4 (two-thirds rounded up)
    assertThat(new Scorer(1).passThreshold()).isEqualTo(1);
    assertThat(new Scorer(2).passThreshold()).isEqualTo(2);
    assertThat(new Scorer(3).passThreshold()).isEqualTo(2);
    assertThat(new Scorer(4).passThreshold()).isEqualTo(3);
    assertThat(new Scorer(5).passThreshold()).isEqualTo(4);
  }

  @Test
  void band() {
    Scorer scorer = new Scorer(3);

    assertThat(scorer.band(3)).isEqualTo(Scorer.Band.STABLE_PASS);
    assertThat(scorer.band(2)).isEqualTo(Scorer.Band.STABLE_PASS);
    assertThat(scorer.band(1)).isEqualTo(Scorer.Band.FLAKY);
    assertThat(scorer.band(0)).isEqualTo(Scorer.Band.STABLE_FAIL);
  }

  @Test
  void scoreGroupsCaseResultsPreservingOrder() {
    CheckResult checkPass = new CheckResult(0, "contains", true, new Outcome.Pass(), 1);
    CheckResult checkFail = new CheckResult(0, "contains", true, new Outcome.Fail("no"), 1);

    List<CaseResult> results =
        List.of(
            new CaseResult("c1", 0, List.of(checkPass)),
            new CaseResult("c2", 0, List.of(checkPass)),
            new CaseResult("c1", 1, List.of(checkPass)),
            new CaseResult("c3", 0, List.of(checkFail)));

    Scorer scorer = new Scorer(3);
    List<Scorer.CaseScore> scores = scorer.score(results);

    assertThat(scores).hasSize(3);
    assertThat(scores.get(0).caseId()).isEqualTo("c1");
    assertThat(scores.get(1).caseId()).isEqualTo("c2");
    assertThat(scores.get(2).caseId()).isEqualTo("c3");
  }

  @Test
  void scoreCounts() {
    CheckResult checkPass = new CheckResult(0, "contains", true, new Outcome.Pass(), 1);
    CheckResult checkFail = new CheckResult(0, "contains", true, new Outcome.Fail("no"), 1);

    List<CaseResult> results =
        List.of(
            new CaseResult("c1", 0, List.of(checkPass)),
            new CaseResult("c1", 1, List.of(checkPass)),
            new CaseResult("c1", 2, List.of(checkFail)));

    Scorer scorer = new Scorer(3);
    List<Scorer.CaseScore> scores = scorer.score(results);

    assertThat(scores).hasSize(1);
    Scorer.CaseScore score = scores.get(0);
    assertThat(score.caseId()).isEqualTo("c1");
    assertThat(score.executions()).isEqualTo(3);
    assertThat(score.passes()).isEqualTo(2);
  }

  @Test
  void invalidConstructorArgs() {
    assertThatThrownBy(() -> new Scorer(0)).isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> new Scorer(3, 0)).isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> new Scorer(3, 4)).isInstanceOf(IllegalArgumentException.class);
  }
}
