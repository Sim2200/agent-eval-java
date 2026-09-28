package io.github.sim2200.agenteval.evaluator;

/** The thing an {@link LlmJudgeEvaluator} talks to. One real implementation, one stub. */
public interface JudgeClient {

  record Verdict(boolean satisfied, String reason) {}

  String name();

  Verdict judge(String input, String output, String rubric);
}
