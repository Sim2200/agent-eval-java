package io.github.sim2200.agenteval.evaluator;

import static org.assertj.core.api.Assertions.*;

import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MustNotFireEvaluatorTest {

  private MustNotFireEvaluator evaluator;

  @BeforeEach
  void setUp() {
    evaluator = new MustNotFireEvaluator();
  }

  @Test
  void phrasePresent() {
    String output = "I can provide a refund for you";
    TestCase testCase = new TestCase("c1", "input", output, null);
    Check check = new Check("must_not_fire", Map.of("phrases", List.of("refund")), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Fail.class);
  }

  @Test
  void phraseAbsent() {
    String output = "Your order is confirmed";
    TestCase testCase = new TestCase("c1", "input", output, null);
    Check check = new Check("must_not_fire", Map.of("phrases", List.of("refund")), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Pass.class);
  }

  @Test
  void patternMatch() {
    String output = "Error code: 500";
    TestCase testCase = new TestCase("c1", "input", output, null);
    Check check = new Check("must_not_fire", Map.of("patterns", List.of("\\d{3}")), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Fail.class);
  }

  @Test
  void invalidPattern() {
    TestCase testCase = new TestCase("c1", "input", "output", null);
    Check check = new Check("must_not_fire", Map.of("patterns", List.of("[invalid(")), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Error.class);
  }

  @Test
  void noPhrasesNoPatternsError() {
    TestCase testCase = new TestCase("c1", "input", "output", null);
    Check check = new Check("must_not_fire", Map.of(), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Error.class);
  }

  @Test
  void caseInsensitivePhraseMatch() {
    String output = "A REFUND has been approved";
    TestCase testCase = new TestCase("c1", "input", output, null);
    Check check = new Check("must_not_fire", Map.of("phrases", List.of("refund")), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Fail.class);
  }

  @Test
  void patternNoMatch() {
    String output = "No error codes";
    TestCase testCase = new TestCase("c1", "input", output, null);
    Check check = new Check("must_not_fire", Map.of("patterns", List.of("\\d{3}")), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Pass.class);
  }
}
