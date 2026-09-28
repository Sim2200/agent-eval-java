package io.github.sim2200.agenteval.evaluator;

import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Asks a judge whether the output satisfies a natural-language {@code rubric}. The judge is
 * injected: {@link GeminiJudgeClient} when {@code GEMINI_API_KEY} is set, otherwise the
 * deterministic {@link StubJudgeClient}, so the suite runs the same way in CI and offline.
 *
 * <p>Judges are the expensive, non-deterministic check, which is why the scorer runs cases N times
 * and bands them; a case that a judge passes twice out of three is "flaky", not "passing".
 */
@Component
public class LlmJudgeEvaluator implements Evaluator {

  private final JudgeClient judge;

  public LlmJudgeEvaluator(JudgeClient judge) {
    this.judge = judge;
  }

  @Override
  public String type() {
    return "llm_judge";
  }

  @Override
  public Outcome evaluate(TestCase testCase, Check check) {
    String rubric = check.param("rubric");
    if (rubric == null || rubric.isBlank()) {
      return new Outcome.Error("llm_judge: 'rubric' is required");
    }
    try {
      JudgeClient.Verdict verdict = judge.judge(testCase.input(), testCase.output(), rubric);
      return verdict.satisfied()
          ? new Outcome.Pass()
          : new Outcome.Fail(
              "judge: " + verdict.reason() + " [" + judge.name().toLowerCase(Locale.ROOT) + "]");
    } catch (RuntimeException e) {
      return new Outcome.Error("llm_judge: " + e.getMessage());
    }
  }
}
