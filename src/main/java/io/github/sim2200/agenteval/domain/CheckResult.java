package io.github.sim2200.agenteval.domain;

/**
 * One check's outcome on one execution of one case.
 *
 * @param checkIndex position of the check in the case
 * @param checkType the evaluator type
 * @param shouldPass the ground truth copied from the check
 * @param outcome what the evaluator returned
 * @param latencyMillis wall time of the evaluator call
 */
public record CheckResult(
    int checkIndex, String checkType, boolean shouldPass, Outcome outcome, long latencyMillis) {

  /** True when the check's fire/no-fire matches the ground truth. */
  public boolean correct() {
    return outcome instanceof Outcome.Error ? false : outcome.fired() == !shouldPass;
  }
}
