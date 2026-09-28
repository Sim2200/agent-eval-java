package io.github.sim2200.agenteval.scoring;

import io.github.sim2200.agenteval.domain.CaseResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Repeat-N banded scoring. A case is executed N times (the judge is not deterministic, and even
 * deterministic checks can hit transient errors). It is then put in a band:
 *
 * <ul>
 *   <li>{@code STABLE_PASS}: passed at least {@code passThreshold} of N executions
 *   <li>{@code FLAKY}: passed at least once but fewer than the threshold
 *   <li>{@code STABLE_FAIL}: never passed
 * </ul>
 *
 * With N=3 and the default threshold of 2, "2 of 3" passes; a single lucky pass is flaky, not a
 * pass. The threshold defaults to a two-thirds majority, rounded up.
 */
public final class Scorer {

  public enum Band {
    STABLE_PASS,
    FLAKY,
    STABLE_FAIL
  }

  /** The score of one case across its N executions. */
  public record CaseScore(String caseId, int executions, int passes, Band band) {
    public double passRate() {
      return executions == 0 ? 0.0 : (double) passes / executions;
    }
  }

  private final int repeat;
  private final int passThreshold;

  public Scorer(int repeat) {
    this(repeat, defaultThreshold(repeat));
  }

  public Scorer(int repeat, int passThreshold) {
    if (repeat < 1) {
      throw new IllegalArgumentException("repeat must be >= 1");
    }
    if (passThreshold < 1 || passThreshold > repeat) {
      throw new IllegalArgumentException("passThreshold must be in [1, repeat]");
    }
    this.repeat = repeat;
    this.passThreshold = passThreshold;
  }

  /** Two-thirds of N, rounded up: 1->1, 2->2, 3->2, 4->3, 5->4. */
  static int defaultThreshold(int repeat) {
    return (int) Math.ceil(repeat * 2.0 / 3.0);
  }

  public int repeat() {
    return repeat;
  }

  public int passThreshold() {
    return passThreshold;
  }

  public Band band(int passes) {
    if (passes >= passThreshold) {
      return Band.STABLE_PASS;
    }
    return passes == 0 ? Band.STABLE_FAIL : Band.FLAKY;
  }

  /** Groups executions by case id and bands each case. Order follows first appearance. */
  public List<CaseScore> score(List<CaseResult> results) {
    Map<String, List<CaseResult>> byCase =
        results.stream()
            .collect(
                Collectors.groupingBy(
                    CaseResult::caseId, java.util.LinkedHashMap::new, Collectors.toList()));
    return byCase.entrySet().stream()
        .map(
            e -> {
              int passes = (int) e.getValue().stream().filter(CaseResult::passed).count();
              return new CaseScore(e.getKey(), e.getValue().size(), passes, band(passes));
            })
        .toList();
  }
}
