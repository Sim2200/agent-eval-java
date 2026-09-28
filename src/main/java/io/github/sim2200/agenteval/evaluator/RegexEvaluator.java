package io.github.sim2200.agenteval.evaluator;

import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Component;

/** Passes when {@code pattern} matches somewhere in the output. Patterns are compiled once. */
@Component
public class RegexEvaluator implements Evaluator {

  private final Map<String, Pattern> cache = new ConcurrentHashMap<>();

  @Override
  public String type() {
    return "regex";
  }

  @Override
  public Outcome evaluate(TestCase testCase, Check check) {
    String pattern = check.param("pattern");
    if (pattern == null || pattern.isEmpty()) {
      return new Outcome.Error("regex: 'pattern' is required");
    }
    try {
      Pattern compiled = cache.computeIfAbsent(pattern, Pattern::compile);
      return compiled.matcher(testCase.output()).find()
          ? new Outcome.Pass()
          : new Outcome.Fail("output does not match /" + pattern + "/");
    } catch (PatternSyntaxException e) {
      return new Outcome.Error("regex: invalid pattern: " + e.getDescription());
    }
  }
}
