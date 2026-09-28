package io.github.sim2200.agenteval.evaluator;

import static org.assertj.core.api.Assertions.*;

import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContainsEvaluatorTest {

  private ContainsEvaluator evaluator;

  @BeforeEach
  void setUp() {
    evaluator = new ContainsEvaluator();
  }

  @Test
  void caseInsensitiveDefault() {
    TestCase testCase = new TestCase("c1", "input", "Hello World", null);
    Check check = new Check("contains", Map.of("text", "hello"), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Pass.class);
  }

  @Test
  void caseSensitiveTrueFail() {
    TestCase testCase = new TestCase("c1", "input", "Hello World", null);
    Check check = new Check("contains", Map.of("text", "hello", "case_sensitive", "true"), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Fail.class);
    assertThat(((Outcome.Fail) result).reason()).contains("hello");
  }

  @Test
  void missingTextReturnsError() {
    TestCase testCase = new TestCase("c1", "input", "output", null);
    Check check = new Check("contains", Map.of(), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Error.class);
  }

  @Test
  void failReasonMentionsText() {
    TestCase testCase = new TestCase("c1", "input", "Some output", null);
    Check check = new Check("contains", Map.of("text", "specific text"), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Fail.class);
    assertThat(((Outcome.Fail) result).reason()).contains("specific text");
  }

  @Test
  void emptyTextReturnsError() {
    TestCase testCase = new TestCase("c1", "input", "output", null);
    Check check = new Check("contains", Map.of("text", ""), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Error.class);
  }
}
