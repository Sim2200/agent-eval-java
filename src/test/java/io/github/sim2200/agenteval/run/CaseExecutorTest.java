package io.github.sim2200.agenteval.run;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.sim2200.agenteval.domain.CaseResult;
import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import io.github.sim2200.agenteval.evaluator.ContainsEvaluator;
import io.github.sim2200.agenteval.evaluator.Evaluator;
import io.github.sim2200.agenteval.evaluator.EvaluatorFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseExecutorTest {

  @Test
  void executeWithRealFactory() {
    EvaluatorFactory factory = new EvaluatorFactory(List.of(new ContainsEvaluator()));

    CaseExecutor executor = new CaseExecutor(factory);

    TestCase testCase =
        new TestCase(
            "c1",
            "input",
            "Hello World",
            List.of(new Check("contains", Map.of("text", "World"), true)));

    CaseResult result = executor.execute(testCase, 0);

    assertThat(result.caseId()).isEqualTo("c1");
    assertThat(result.repeatIndex()).isEqualTo(0);
    assertThat(result.checks()).hasSize(1);
    assertThat(result.checks().get(0).outcome()).isInstanceOf(Outcome.Pass.class);
  }

  @Test
  void evaluatorThrowsReturnsError(@Mock EvaluatorFactory factory, @Mock Evaluator mockEvaluator) {
    when(factory.forType("mock")).thenReturn(mockEvaluator);
    when(mockEvaluator.evaluate(any(), any())).thenThrow(new RuntimeException("evaluator error"));

    CaseExecutor executor = new CaseExecutor(factory);

    TestCase testCase =
        new TestCase(
            "c1",
            "input",
            "output",
            List.of(
                new Check("contains", Map.of("text", "test"), true),
                new Check("mock", Map.of(), true)));

    CaseResult result = executor.execute(testCase, 0);

    // Second check should be Error due to throw, but first should still evaluate
    assertThat(result.checks()).hasSize(2);
    assertThat(result.checks().get(1).outcome()).isInstanceOf(Outcome.Error.class);
  }

  @Test
  void latencyNonNegative() {
    EvaluatorFactory factory = new EvaluatorFactory(List.of(new ContainsEvaluator()));

    CaseExecutor executor = new CaseExecutor(factory);

    TestCase testCase =
        new TestCase(
            "c1",
            "input",
            "output",
            List.of(new Check("contains", Map.of("text", "output"), true)));

    CaseResult result = executor.execute(testCase, 0);

    assertThat(result.checks().get(0).latencyMillis()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void allChecksEvaluated() {
    EvaluatorFactory factory = new EvaluatorFactory(List.of(new ContainsEvaluator()));

    CaseExecutor executor = new CaseExecutor(factory);

    TestCase testCase =
        new TestCase(
            "c1",
            "input",
            "Hello World",
            List.of(
                new Check("contains", Map.of("text", "Hello"), true),
                new Check("contains", Map.of("text", "World"), true),
                new Check("contains", Map.of("text", "foo"), true)));

    CaseResult result = executor.execute(testCase, 0);

    assertThat(result.checks()).hasSize(3);
    assertThat(result.checks().get(0).outcome()).isInstanceOf(Outcome.Pass.class);
    assertThat(result.checks().get(1).outcome()).isInstanceOf(Outcome.Pass.class);
    assertThat(result.checks().get(2).outcome()).isInstanceOf(Outcome.Fail.class);
  }
}
