package io.github.sim2200.agenteval.run;

import static org.assertj.core.api.Assertions.*;

import io.github.sim2200.agenteval.domain.CaseResult;
import io.github.sim2200.agenteval.domain.TestCase;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ParallelRunnerTest {

  @Test
  void parallelRunWithConcurrency() {
    // Create 50 cases
    List<TestCase> cases = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      cases.add(new TestCase("c" + i, "input", "output", List.of()));
    }

    AtomicInteger maxInflight = new AtomicInteger(0);
    AtomicInteger currentInflight = new AtomicInteger(0);

    List<CaseResult> results =
        ParallelRunner.run(
            cases,
            2,
            4,
            (testCase, repeatIndex) -> {
              int current = currentInflight.incrementAndGet();
              int max = maxInflight.getAndUpdate(m -> Math.max(m, current));
              try {
                Thread.sleep(5);
                return new CaseResult(testCase.id(), repeatIndex, List.of());
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
              } finally {
                currentInflight.decrementAndGet();
              }
            });

    assertThat(results).hasSize(100);
    assertThat(maxInflight.get()).isLessThanOrEqualTo(4);
  }

  @Test
  void parallelRunAllCombinations() {
    List<TestCase> cases =
        List.of(
            new TestCase("c1", "input", "output", List.of()),
            new TestCase("c2", "input", "output", List.of()));

    List<CaseResult> results =
        ParallelRunner.run(
            cases,
            2,
            2,
            (testCase, repeatIndex) -> new CaseResult(testCase.id(), repeatIndex, List.of()));

    assertThat(results).hasSize(4);

    Set<String> combinations = new HashSet<>();
    for (CaseResult r : results) {
      combinations.add(r.caseId() + "-" + r.repeatIndex());
    }

    assertThat(combinations).containsExactlyInAnyOrder("c1-0", "c1-1", "c2-0", "c2-1");
  }

  @Test
  void functionThrows() {
    List<TestCase> cases = List.of(new TestCase("c1", "input", "output", List.of()));

    assertThatThrownBy(
            () ->
                ParallelRunner.run(
                    cases,
                    1,
                    1,
                    (testCase, repeatIndex) -> {
                      throw new RuntimeException("test error");
                    }))
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(RuntimeException.class);
  }

  @Test
  void concurrencyZero() {
    List<TestCase> cases = List.of(new TestCase("c1", "input", "output", List.of()));

    assertThatThrownBy(() -> ParallelRunner.run(cases, 1, 0, (testCase, repeatIndex) -> null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("concurrency");
  }

  @Test
  void concurrencyOne() {
    List<TestCase> cases =
        List.of(
            new TestCase("c1", "input", "output", List.of()),
            new TestCase("c2", "input", "output", List.of()));

    List<CaseResult> results =
        ParallelRunner.run(
            cases,
            1,
            1,
            (testCase, repeatIndex) -> new CaseResult(testCase.id(), repeatIndex, List.of()));

    assertThat(results).hasSize(2);
  }
}
