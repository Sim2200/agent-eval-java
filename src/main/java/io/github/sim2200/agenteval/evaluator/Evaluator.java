package io.github.sim2200.agenteval.evaluator;

import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;

/**
 * Strategy interface: one implementation per check type. The runner never knows which one it is
 * holding, which is what lets a suite mix cheap deterministic checks with an LLM judge.
 */
public interface Evaluator {

  /** The {@code type} string used in suite YAML to select this evaluator. */
  String type();

  /** Evaluates one check against one case. Must never throw: return {@link Outcome.Error}. */
  Outcome evaluate(TestCase testCase, Check check);
}
