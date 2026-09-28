package io.github.sim2200.agenteval.run;

import io.github.sim2200.agenteval.domain.CaseResult;
import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.CheckResult;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import io.github.sim2200.agenteval.evaluator.Evaluator;
import io.github.sim2200.agenteval.evaluator.EvaluatorFactory;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Runs every check of one case once. Evaluator exceptions become {@link Outcome.Error}. */
@Component
public class CaseExecutor {

  private final EvaluatorFactory factory;

  public CaseExecutor(EvaluatorFactory factory) {
    this.factory = factory;
  }

  public CaseResult execute(TestCase testCase, int repeatIndex) {
    List<CheckResult> results = new ArrayList<>(testCase.checks().size());
    for (int i = 0; i < testCase.checks().size(); i++) {
      Check check = testCase.checks().get(i);
      long start = System.nanoTime();
      Outcome outcome;
      try {
        Evaluator evaluator = factory.forType(check.type());
        outcome = evaluator.evaluate(testCase, check);
      } catch (RuntimeException e) {
        outcome =
            new Outcome.Error(
                e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
      }
      long millis = (System.nanoTime() - start) / 1_000_000;
      results.add(new CheckResult(i, check.type(), check.shouldPass(), outcome, millis));
    }
    return new CaseResult(testCase.id(), repeatIndex, results);
  }
}
