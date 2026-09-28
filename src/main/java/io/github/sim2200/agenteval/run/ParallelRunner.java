package io.github.sim2200.agenteval.run;

import io.github.sim2200.agenteval.domain.CaseResult;
import io.github.sim2200.agenteval.domain.TestCase;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;

/**
 * Executes (case, repeat) pairs concurrently. One virtual thread per task, bounded by a semaphore
 * so at most {@code concurrency} evaluations are in flight: virtual threads are cheap, but the
 * judge behind them has a rate limit, and a bound is what keeps a 10,000-case suite from turning
 * into 10,000 simultaneous HTTP calls.
 *
 * <p>The executor is a parameter so the benchmark can run the same code on a platform thread pool.
 */
public final class ParallelRunner {

  private ParallelRunner() {}

  public static List<CaseResult> run(
      List<TestCase> cases,
      int repeat,
      int concurrency,
      BiFunction<TestCase, Integer, CaseResult> executeOne) {
    try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
      return run(cases, repeat, concurrency, executeOne, pool);
    }
  }

  public static List<CaseResult> run(
      List<TestCase> cases,
      int repeat,
      int concurrency,
      BiFunction<TestCase, Integer, CaseResult> executeOne,
      ExecutorService pool) {
    if (concurrency < 1) {
      throw new IllegalArgumentException("concurrency must be >= 1");
    }
    Semaphore permits = new Semaphore(concurrency);
    List<Future<CaseResult>> futures = new ArrayList<>(cases.size() * repeat);
    for (int r = 0; r < repeat; r++) {
      final int repeatIndex = r;
      for (TestCase testCase : cases) {
        futures.add(
            pool.submit(
                () -> {
                  permits.acquire();
                  try {
                    return executeOne.apply(testCase, repeatIndex);
                  } finally {
                    permits.release();
                  }
                }));
      }
    }
    List<CaseResult> results = new ArrayList<>(futures.size());
    for (Future<CaseResult> f : futures) {
      try {
        results.add(f.get());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("run interrupted", e);
      } catch (ExecutionException e) {
        throw new IllegalStateException("case execution failed: " + e.getCause(), e.getCause());
      }
    }
    return results;
  }
}
