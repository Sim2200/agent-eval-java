package io.github.sim2200.agenteval.benchmark;

import io.github.sim2200.agenteval.domain.CaseResult;
import io.github.sim2200.agenteval.domain.Check;
import io.github.sim2200.agenteval.domain.CheckResult;
import io.github.sim2200.agenteval.domain.Outcome;
import io.github.sim2200.agenteval.domain.TestCase;
import io.github.sim2200.agenteval.run.ParallelRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Virtual threads vs a fixed platform pool on a simulated I/O-bound judge.
 *
 * <p>Each case sleeps a uniformly random 50-200 ms (a stand-in for an HTTP call to a judge) and
 * records its own latency. The same {@link ParallelRunner} code runs on three executors with the
 * same semaphore bound: virtual threads, a platform pool of 32 threads (a typical default), and a
 * platform pool as large as the bound. Three timed repeats per configuration after one warm-up; the
 * median is reported. No JMH: the unit of work is 50-200 ms, so JIT and allocation noise are
 * irrelevant at this scale and a plain timed harness is honest enough.
 *
 * <pre>
 * java -cp target/classes io.github.sim2200.agenteval.benchmark.ThreadBenchmark [cases] [bound] [repeats]
 * </pre>
 *
 * Writes results/benchmark.json.
 */
public final class ThreadBenchmark {

  private ThreadBenchmark() {}

  record Sample(long millis) {}

  static CaseResult simulatedJudge(TestCase c, int repeat, Random rnd, List<Long> latencies) {
    long start = System.nanoTime();
    try {
      Thread.sleep(50 + rnd.nextInt(151));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    long millis = (System.nanoTime() - start) / 1_000_000;
    synchronized (latencies) {
      latencies.add(millis);
    }
    return new CaseResult(
        c.id(), repeat, List.of(new CheckResult(0, "sim", true, new Outcome.Pass(), millis)));
  }

  static Map<String, Object> timed(
      String name, ExecutorService pool, List<TestCase> cases, int bound, boolean close) {
    List<Long> latencies = new ArrayList<>(cases.size());
    Random rnd = new Random(42);
    AtomicLong seq = new AtomicLong();
    long start = System.nanoTime();
    ParallelRunner.run(cases, 1, bound, (c, r) -> simulatedJudge(c, r, rnd, latencies), pool);
    long wallMillis = (System.nanoTime() - start) / 1_000_000;
    if (close) {
      pool.close();
    }
    long[] sorted = latencies.stream().mapToLong(Long::longValue).sorted().toArray();
    double p95 = sorted[(int) Math.min(sorted.length - 1, Math.floor(sorted.length * 0.95))];
    return Map.of(
        "executor", name,
        "cases", cases.size(),
        "bound", bound,
        "wall_millis", wallMillis,
        "cases_per_second", Math.round(cases.size() * 1000.0 / wallMillis * 10) / 10.0,
        "p95_case_millis", p95,
        "mean_case_millis", Math.round(Arrays.stream(sorted).average().orElse(0) * 10) / 10.0);
  }

  public static void main(String[] args) throws IOException {
    int n = args.length > 0 ? Integer.parseInt(args[0]) : 2000;
    int bound = args.length > 1 ? Integer.parseInt(args[1]) : 200;
    int repeats = args.length > 2 ? Integer.parseInt(args[2]) : 3;
    List<TestCase> cases = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      cases.add(new TestCase("c" + i, "in", "out", List.of(new Check("sim", Map.of(), true))));
    }
    Map<String, List<Map<String, Object>>> results = new java.util.LinkedHashMap<>();
    String[] names = {"virtual_threads", "platform_pool_32", "platform_pool_" + bound};
    for (String name : names) {
      List<Map<String, Object>> runs = new ArrayList<>();
      for (int r = 0; r <= repeats; r++) { // r == 0 is the warm-up, discarded
        ExecutorService pool =
            switch (name) {
              case "virtual_threads" -> Executors.newVirtualThreadPerTaskExecutor();
              case "platform_pool_32" -> Executors.newFixedThreadPool(32);
              default -> Executors.newFixedThreadPool(bound);
            };
        Map<String, Object> m = timed(name, pool, cases, bound, true);
        if (r > 0) {
          runs.add(m);
        }
        System.out.printf(
            "%-20s run %d: %s cases/s, p95 %s ms, wall %s ms%n",
            name, r, m.get("cases_per_second"), m.get("p95_case_millis"), m.get("wall_millis"));
      }
      results.put(name, runs);
    }
    Map<String, Object> out = new java.util.LinkedHashMap<>();
    out.put("cases", n);
    out.put("semaphore_bound", bound);
    out.put("repeats", repeats);
    out.put("simulated_judge", "Thread.sleep(50..200 ms) per case, seed 42");
    out.put("java", System.getProperty("java.version"));
    out.put("cpus", Runtime.getRuntime().availableProcessors());
    out.put("runs", results);
    Map<String, Object> median = new java.util.LinkedHashMap<>();
    results.forEach(
        (name, runs) -> {
          double[] cps =
              runs.stream()
                  .mapToDouble(m -> ((Number) m.get("cases_per_second")).doubleValue())
                  .sorted()
                  .toArray();
          double[] p95 =
              runs.stream()
                  .mapToDouble(m -> ((Number) m.get("p95_case_millis")).doubleValue())
                  .sorted()
                  .toArray();
          median.put(
              name,
              Map.of(
                  "cases_per_second_median",
                  cps[cps.length / 2],
                  "p95_case_millis_median",
                  p95[p95.length / 2]));
        });
    out.put("median", median);
    Files.createDirectories(Path.of("results"));
    Files.writeString(
        Path.of("results/benchmark.json"),
        new com.fasterxml.jackson.databind.ObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(out));
    System.out.println("wrote results/benchmark.json");
  }
}
