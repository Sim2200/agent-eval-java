package io.github.sim2200.agenteval.evaluator;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LlmJudgeEvaluatorTest {

  @Mock private JudgeClient judge;

  private LlmJudgeEvaluator evaluator;

  @BeforeEach
  void setUp() {
    evaluator = new LlmJudgeEvaluator(judge);
  }

  @Test
  void satisfiedPass() {
    when(judge.judge("input", "output", "rubric"))
        .thenReturn(new JudgeClient.Verdict(true, "reason"));

    TestCase testCase = new TestCase("c1", "input", "output", null);
    Check check = new Check("llm_judge", Map.of("rubric", "rubric"), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Pass.class);
  }

  @Test
  void notSatisfiedFail() {
    when(judge.judge("input", "output", "rubric"))
        .thenReturn(new JudgeClient.Verdict(false, "not good"));
    when(judge.name()).thenReturn("test-judge");

    TestCase testCase = new TestCase("c1", "input", "output", null);
    Check check = new Check("llm_judge", Map.of("rubric", "rubric"), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Fail.class);
    assertThat(((Outcome.Fail) result).reason()).contains("not good");
  }

  @Test
  void clientThrowsRuntimeException() {
    when(judge.judge("input", "output", "rubric")).thenThrow(new RuntimeException("API error"));

    TestCase testCase = new TestCase("c1", "input", "output", null);
    Check check = new Check("llm_judge", Map.of("rubric", "rubric"), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Error.class);
  }

  @Test
  void missingRubricError() {
    TestCase testCase = new TestCase("c1", "input", "output", null);
    Check check = new Check("llm_judge", Map.of(), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Error.class);
    verify(judge, never()).judge(anyString(), anyString(), anyString());
  }

  @Test
  void blankRubricError() {
    TestCase testCase = new TestCase("c1", "input", "output", null);
    Check check = new Check("llm_judge", Map.of("rubric", "  "), true);

    Outcome result = evaluator.evaluate(testCase, check);

    assertThat(result).isInstanceOf(Outcome.Error.class);
  }
}
