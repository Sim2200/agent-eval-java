package io.github.sim2200.agenteval.evaluator;

import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * A negative assertion: the output must contain none of the {@code phrases} (case-insensitive) and
 * match none of the {@code patterns}. It fires when a forbidden thing appears: a hallucinated
 * refund, a leaked internal note, an apology loop. The report's precision/recall of fires is mostly
 * about this evaluator, because a monitoring rule that fires on the wrong things is as costly as
 * one that misses.
 */
@Component
public class MustNotFireEvaluator implements Evaluator {

  @Override
  public String type() {
    return "must_not_fire";
  }

  @Override
  @SuppressWarnings("unchecked")
  public Outcome evaluate(TestCase testCase, Check check) {
    List<String> phrases = (List<String>) check.params().getOrDefault("phrases", List.of());
    List<String> patterns = (List<String>) check.params().getOrDefault("patterns", List.of());
    if (phrases.isEmpty() && patterns.isEmpty()) {
      return new Outcome.Error("must_not_fire: 'phrases' or 'patterns' is required");
    }
    String lower = testCase.output().toLowerCase(Locale.ROOT);
    for (String phrase : phrases) {
      if (lower.contains(phrase.toLowerCase(Locale.ROOT))) {
        return new Outcome.Fail("forbidden phrase present: \"" + phrase + "\"");
      }
    }
    for (String pattern : patterns) {
      try {
        if (Pattern.compile(pattern).matcher(testCase.output()).find()) {
          return new Outcome.Fail("forbidden pattern matched: /" + pattern + "/");
        }
      } catch (RuntimeException e) {
        return new Outcome.Error("must_not_fire: invalid pattern /" + pattern + "/");
      }
    }
    return new Outcome.Pass();
  }
}
