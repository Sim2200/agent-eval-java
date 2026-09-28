package io.github.sim2200.agenteval.domain;

import java.util.List;

/**
 * One case in a suite: what the agent was given, what it produced, and the checks to apply.
 *
 * @param id unique within the suite
 * @param input the user or task input the agent saw
 * @param output the agent's output, or a recorded transcript
 * @param checks the assertions to evaluate against {@code output}
 */
public record TestCase(String id, String input, String output, List<Check> checks) {

  public TestCase {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("case id is required");
    }
    output = output == null ? "" : output;
    checks = checks == null ? List.of() : List.copyOf(checks);
  }
}
