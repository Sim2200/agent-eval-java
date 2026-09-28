package io.github.sim2200.agenteval.run;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.sim2200.agenteval.domain.CaseResult;
import io.github.sim2200.agenteval.domain.CheckResult;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.scoring.Scorer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a run reports: banded case scores plus, per check type, the precision and recall of fires.
 *
 * <p>A fire is a {@link Outcome.Fail}. Against the ground truth on each check ({@code
 * should_pass}), a fire on a case that should pass is a false fire (false positive); no fire on a
 * case that should fail is a missed fire (false negative). Errors are counted separately and are
 * never a fire.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RunSummary(
    int cases,
    int executions,
    int repeat,
    int passThreshold,
    int stablePass,
    int flaky,
    int stableFail,
    double passRate,
    long wallMillis,
    List<Scorer.CaseScore> caseScores,
    Map<String, CheckMetrics> checks) {

  /** Fire precision/recall for one check type across all executions. */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record CheckMetrics(
      int evaluations, int truePositives, int falsePositives, int falseNegatives, int errors) {
    @JsonProperty("precision")
    public double precision() {
      int fires = truePositives + falsePositives;
      return fires == 0 ? 1.0 : (double) truePositives / fires;
    }

    @JsonProperty("recall")
    public double recall() {
      int shouldFire = truePositives + falseNegatives;
      return shouldFire == 0 ? 1.0 : (double) truePositives / shouldFire;
    }
  }

  public static RunSummary of(List<CaseResult> results, Scorer scorer, long wallMillis) {
    List<Scorer.CaseScore> scores = scorer.score(results);
    int stablePass = 0;
    int flaky = 0;
    int stableFail = 0;
    for (Scorer.CaseScore s : scores) {
      switch (s.band()) {
        case STABLE_PASS -> stablePass++;
        case FLAKY -> flaky++;
        case STABLE_FAIL -> stableFail++;
      }
    }
    Map<String, int[]> counts = new LinkedHashMap<>();
    for (CaseResult cr : results) {
      for (CheckResult c : cr.checks()) {
        int[] k = counts.computeIfAbsent(c.checkType(), t -> new int[5]);
        k[0]++;
        if (c.outcome() instanceof Outcome.Error) {
          k[4]++;
        } else if (c.outcome().fired() && !c.shouldPass()) {
          k[1]++;
        } else if (c.outcome().fired()) {
          k[2]++;
        } else if (!c.shouldPass()) {
          k[3]++;
        }
      }
    }
    Map<String, CheckMetrics> checks = new LinkedHashMap<>();
    counts.forEach((t, k) -> checks.put(t, new CheckMetrics(k[0], k[1], k[2], k[3], k[4])));
    double passRate = scores.isEmpty() ? 0.0 : (double) stablePass / scores.size();
    return new RunSummary(
        scores.size(),
        results.size(),
        scorer.repeat(),
        scorer.passThreshold(),
        stablePass,
        flaky,
        stableFail,
        passRate,
        wallMillis,
        new ArrayList<>(scores),
        checks);
  }
}
