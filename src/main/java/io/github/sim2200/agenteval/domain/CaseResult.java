package io.github.sim2200.agenteval.domain;

import java.util.List;

/**
 * All checks of one case on one execution (one of the repeat-N runs).
 *
 * @param caseId the case
 * @param repeatIndex 0-based execution number
 * @param checks the check results in case order
 */
public record CaseResult(String caseId, int repeatIndex, List<CheckResult> checks) {

  public CaseResult {
    checks = checks == null ? List.of() : List.copyOf(checks);
  }

  /** The case passes an execution when every check's outcome matched its ground truth. */
  public boolean passed() {
    return !checks.isEmpty() && checks.stream().allMatch(CheckResult::correct);
  }
}
