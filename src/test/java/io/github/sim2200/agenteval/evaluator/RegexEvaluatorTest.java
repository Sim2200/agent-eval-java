package io.github.sim2200.agenteval.evaluator;

import static org.assertj.core.api.Assertions.*;

import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegexEvaluatorTest {

  private RegexEvaluator evaluator;

  @BeforeEach
  void setUp() {
    evaluator = new RegexEvaluator();
  }

  @Test
  void matchPass() {
    TestCase testCase = new TestCase("c1", "input", "Order ID: 12345", null);
    Check check = new Check("regex", Map.of("pattern", "\\d+"), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Pass.class);
  }

  @Test
  void noMatchFail() {
    TestCase testCase = new TestCase("c1", "input", "No digits here", null);
    Check check = new Check("regex", Map.of("pattern", "\\d+"), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Fail.class);
  }

  @Test
  void invalidPatternError() {
    TestCase testCase = new TestCase("c1", "input", "output", null);
    Check check = new Check("regex", Map.of("pattern", "[invalid("), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Error.class);
  }

  @Test
  void patternCacheReuse() {
    TestCase testCase = new TestCase("c1", "input", "abc 123", null);
    Check check = new Check("regex", Map.of("pattern", "[0-9]+"), true);

    // First evaluation
    Outcome result1 = evaluator.evaluate(testCase, check);
    assertThat(result1).isInstanceOf(Outcome.Pass.class);

    // Second evaluation with same pattern should reuse cache
    Outcome result2 = evaluator.evaluate(testCase, check);
    assertThat(result2).isInstanceOf(Outcome.Pass.class);
  }

  @Test
  void missingPatternError() {
    TestCase testCase = new TestCase("c1", "input", "output", null);
    Check check = new Check("regex", Map.of(), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Error.class);
  }
}
