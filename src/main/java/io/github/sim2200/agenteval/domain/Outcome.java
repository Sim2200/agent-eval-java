package io.github.sim2200.agenteval.domain;

/**
 * The result of one check on one case. Sealed so every consumer has to handle exactly these three
 * shapes and the compiler tells us when a fourth is added.
 */
public sealed interface Outcome permits Outcome.Pass, Outcome.Fail, Outcome.Error {

  /** The check held. */
  record Pass() implements Outcome {}

  /** The check did not hold; {@code reason} says why, for the report. */
  record Fail(String reason) implements Outcome {}

  /**
   * The check could not be evaluated (bad regex, judge unavailable, ...). Never counts as a pass.
   */
  record Error(String message) implements Outcome {}

  default boolean passed() {
    return this instanceof Pass;
  }

  /**
   * A check "fires" when it flags a problem: a Fail. Errors are reported separately, not as fires.
   */
  default boolean fired() {
    return this instanceof Fail;
  }
}
