package io.github.sim2200.agenteval.evaluator;

import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Passes when the output contains {@code text}. {@code case_sensitive} defaults to false. */
@Component
public class ContainsEvaluator implements Evaluator {

  @Override
  public String type() {
    return "contains";
  }

  @Override
  public Outcome evaluate(TestCase testCase, Check check) {
    String text = check.param("text");
    if (text == null || text.isEmpty()) {
      return new Outcome.Error("contains: 'text' is required");
    }
    boolean caseSensitive = Boolean.parseBoolean(check.param("case_sensitive"));
    String haystack =
        caseSensitive ? testCase.output() : testCase.output().toLowerCase(Locale.ROOT);
    String needle = caseSensitive ? text : text.toLowerCase(Locale.ROOT);
    return haystack.contains(needle)
        ? new Outcome.Pass()
        : new Outcome.Fail("output does not contain \"" + text + "\"");
  }
}
