package io.github.sim2200.agenteval.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class OutcomeAndResultsTest {

  @Test
  void outcomePassedAndFired() {
    Outcome pass = new Outcome.Pass();
    assertThat(pass.passed()).isTrue();
    assertThat(pass.fired()).isFalse();
  }

  @Test
  void outcomeFailFiredButNotPassed() {
    Outcome fail = new Outcome.Fail("test reason");
    assertThat(fail.fired()).isTrue();
    assertThat(fail.passed()).isFalse();
  }

  @Test
  void outcomeErrorNotPassedNotFired() {
    Outcome error = new Outcome.Error("test error");
    assertThat(error.passed()).isFalse();
    assertThat(error.fired()).isFalse();
  }

  @Test
  void checkResultCorrect() {
    // Pass with shouldPass true -> true
    CheckResult passCorrect = new CheckResult(0, "contains", true, new Outcome.Pass(), 1);
    assertThat(passCorrect.correct()).isTrue();

    // Fail with shouldPass false (should fire) -> true
    CheckResult failCorrect = new CheckResult(0, "contains", false, new Outcome.Fail("reason"), 1);
    assertThat(failCorrect.correct()).isTrue();

    // Fail with shouldPass true (shouldn't fire) -> false
    CheckResult failIncorrect = new CheckResult(0, "contains", true, new Outcome.Fail("reason"), 1);
    assertThat(failIncorrect.correct()).isFalse();

    // Error -> false always
    CheckResult errorResult = new CheckResult(0, "contains", true, new Outcome.Error("error"), 1);
    assertThat(errorResult.correct()).isFalse();
  }

  @Test
  void caseResultPassedEmptyChecks() {
    CaseResult empty = new CaseResult("c1", 0, List.of());
    assertThat(empty.passed()).isFalse();
  }

  @Test
  void caseResultPassedAllChecksPassed() {
    List<CheckResult> checks =
        List.of(
            new CheckResult(0, "contains", true, new Outcome.Pass(), 1),
            new CheckResult(1, "regex", true, new Outcome.Pass(), 2));
    CaseResult result = new CaseResult("c1", 0, checks);
    assertThat(result.passed()).isTrue();
  }

  @Test
  void caseResultNotPassedOneCheckFailed() {
    List<CheckResult> checks =
        List.of(
            new CheckResult(0, "contains", true, new Outcome.Pass(), 1),
            new CheckResult(1, "regex", true, new Outcome.Fail("no match"), 2));
    CaseResult result = new CaseResult("c1", 0, checks);
    assertThat(result.passed()).isFalse();
  }

  @Test
  void checkConstructorRejectsBlankType() {
    assertThatThrownBy(() -> new Check("", null, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("type");

    assertThatThrownBy(() -> new Check("  ", null, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("type");
  }

  @Test
  void checkConstructorCopiesParams() {
    java.util.Map<String, Object> original = new java.util.HashMap<>();
    original.put("text", "hello");
    Check check = new Check("contains", original, true);

    // Modify the original
    original.put("text", "world");

    // Check should have the original value
    assertThat(check.param("text")).isEqualTo("hello");
  }

  @Test
  void testCaseNullOutputBecomesEmpty() {
    TestCase tc = new TestCase("c1", "input", null, null);
    assertThat(tc.output()).isEmpty();
  }

  @Test
  void testCaseNullChecksBecomesEmpty() {
    TestCase tc = new TestCase("c1", "input", "output", null);
    assertThat(tc.checks()).isEmpty();
  }
}
