package io.github.sim2200.agenteval.run;

import io.github.sim2200.agenteval.scoring.Scorer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The regression diff between two runs of the same suite: which cases changed band. "Regressed"
 * means the band got worse (pass -> flaky, pass -> fail, flaky -> fail); "improved" the reverse.
 */
public record RunDiff(
    long baseRunId,
    long otherRunId,
    int compared,
    List<Flip> regressed,
    List<Flip> improved,
    List<String> onlyInBase,
    List<String> onlyInOther) {

  public record Flip(String caseId, Scorer.Band before, Scorer.Band after) {}

  public static RunDiff of(
      long baseRunId, List<Scorer.CaseScore> base, long otherRunId, List<Scorer.CaseScore> other) {
    Map<String, Scorer.Band> a =
        base.stream().collect(Collectors.toMap(Scorer.CaseScore::caseId, Scorer.CaseScore::band));
    Map<String, Scorer.Band> b =
        other.stream().collect(Collectors.toMap(Scorer.CaseScore::caseId, Scorer.CaseScore::band));
    List<Flip> regressed = new ArrayList<>();
    List<Flip> improved = new ArrayList<>();
    int compared = 0;
    for (var e : a.entrySet()) {
      Scorer.Band after = b.get(e.getKey());
      if (after == null) {
        continue;
      }
      compared++;
      int delta =
          after.ordinal() - e.getValue().ordinal(); // STABLE_PASS=0 < FLAKY=1 < STABLE_FAIL=2
      if (delta > 0) {
        regressed.add(new Flip(e.getKey(), e.getValue(), after));
      } else if (delta < 0) {
        improved.add(new Flip(e.getKey(), e.getValue(), after));
      }
    }
    List<String> onlyInBase = a.keySet().stream().filter(k -> !b.containsKey(k)).sorted().toList();
    List<String> onlyInOther = b.keySet().stream().filter(k -> !a.containsKey(k)).sorted().toList();
    regressed.sort((x, y) -> x.caseId().compareTo(y.caseId()));
    improved.sort((x, y) -> x.caseId().compareTo(y.caseId()));
    return new RunDiff(
        baseRunId, otherRunId, compared, regressed, improved, onlyInBase, onlyInOther);
  }
}
