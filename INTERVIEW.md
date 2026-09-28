# Interview notes: why it is built this way

Plain-words answers to the questions this project invites. Numbers come from `results/*.json`.

## What the service does, in one breath

You upload a suite: a YAML list of cases, each with an agent's input, its output, and checks. You
run the suite N times with a concurrency limit. Every check returns Pass, Fail or Error. A case
passes an execution when every check agrees with its ground truth (`should_pass`). Cases are then
banded across the N executions (stable pass, flaky, stable fail), and the run reports per check
type how precise and how complete its "fires" were. Two runs can be diffed to see which cases
changed band: that is the regression signal.

## Why the strategy pattern for evaluators

Five kinds of check exist today (contains, regex, JSON schema, must-not-fire, LLM judge) and the
sixth will come. The runner should not care which one it is holding. So `Evaluator` is an
interface with `type()` and `evaluate(testCase, check)`, each check type is one class, and
`EvaluatorFactory` maps the YAML `type` string to the instance. Spring injects every `Evaluator`
bean into the factory, so adding a check type is one new class with `@Component`; the parser,
runner, scorer, API and persistence do not change. The alternative, a switch over type strings in
the runner, would put every evaluator's details in one method and make each new type a change to
shared code.

The factory also fails loudly: an unknown type throws with the list of known types, and that
surfaces as HTTP 400 with the message, instead of silently passing a check nobody evaluated.

## Why a sealed interface for outcomes

`Outcome` is `sealed` with exactly three records: `Pass`, `Fail(reason)`, `Error(message)`. Two
reasons. First, the compiler now knows the full set: a `switch` over an `Outcome` must handle all
three, and if someone adds a fourth the build breaks everywhere it matters rather than at runtime.
Second, the three are not interchangeable and the code should say so: an Error is *not* a Fail. A
regex that cannot compile has not found a problem in the agent's output, it has found a problem in
the suite, and counting it as a fire would inflate false positives. `Outcome.fired()` is true only
for `Fail`; errors are counted in their own column.

Records rather than classes for `TestCase`, `Check`, `CheckResult`, `CaseResult`: they are values.
Immutable, equals/hashCode for free, compact constructors validate (`Check` requires a type and
copies its params map), and they serialise cleanly to JSON for the run summary.

## Why must-not-fire is its own evaluator

A monitoring rule has two failure modes: firing when it should not (a false alarm somebody has to
triage) and staying silent when it should fire (a real problem that ships). `MustNotFireEvaluator`
is a negative assertion: the output must contain none of the listed phrases or patterns. It fires
when a forbidden thing appears. Suites mark deliberate negative examples with `should_pass: false`,
so the run can compute, per check type, precision (of the fires, how many were right) and recall
(of the cases that should have fired, how many did). Those two numbers are what you tune a rule
set against, and they are why the summary is not just "N passed".

## Why repeat-N banded scoring

An LLM judge is not deterministic, and even deterministic checks can hit transient errors. A single
run cannot tell a solid pass from a lucky one. So every case runs N times (default 3) and is banded:
stable pass if it passed at least the threshold (two-thirds of N, rounded up, so 2 of 3), stable
fail if it never passed, flaky in between. Flaky is the interesting band: it is where prompt
changes and judge drift show up first. The threshold is a constructor parameter, so a stricter
suite can ask for 3 of 3.

## Why virtual threads, and what the benchmark showed

A run is I/O-bound: the judge is an HTTP call, and even the deterministic checks are cheap. What
limits throughput is how many evaluations can be in flight at once, and what limits *that* is the
downstream service, not our CPU. So the design is "one virtual thread per (case, repeat), bounded
by a semaphore": the semaphore is set to what the judge can take, and threads are free.

The benchmark (`ThreadBenchmark`, a simulated judge that sleeps 50-200 ms per case) makes the point
concretely; see `results/benchmark_bound200.json` and `results/benchmark_bound1000.json` for the
exact numbers quoted in the README:

- With the bound at 200, virtual threads and a 200-thread platform pool give the same throughput
  and the same p95. That is expected: when you can afford a thread per permit, platform threads
  are fine. The difference is that the platform pool had to be sized to the bound by hand; the
  virtual-thread version just used the bound.
- A 32-thread platform pool (a common default) gets a fraction of the throughput at the same
  bound, because only 32 of the 200 permits can ever be in use. This is the everyday failure mode:
  the limit you meant (200 concurrent judge calls) and the limit you got (32 threads) are
  different numbers.
- At a bound of 1,000 the platform pool is 1,000 OS threads; virtual threads still cost nothing
  to create, and the semaphore is still the only knob.

Limits of the benchmark: the "judge" is `Thread.sleep`, so it measures scheduling, not a real
network; the cases are uniform; it ran on a laptop with other processes; and three repeats give a
median, not a distribution. It shows the shape of the trade-off, not a production number.

## Why synchronous runs, and what I would change at scale

`POST /runs` executes the suite and returns the summary. For suites of a few hundred cases with
the stub judge that takes well under a second, and it keeps the API simple and testable. At scale
the first change is a queue: accept the run, return its id with status RUNNING, execute on a
worker, and let `GET /runs/{id}` report progress. The entity already has RUNNING/FINISHED/FAILED
for that reason. The second change is streaming case results to the database as they finish rather
than in one batch at the end, so a crash mid-run keeps partial results. The third is caching judge
verdicts by (rubric, input, output) hash, since suites re-run the same cases N times and across
runs.

## Why PostgreSQL with Flyway and JPA

Runs must outlive the process (a diff between last week's run and today's is the whole point), and
the schema must evolve without hand-applied SQL. Flyway versions the schema; JPA maps the three
tables; `ddl-auto: validate` makes the app refuse to start if the entities and the migration
disagree, which catches the classic "forgot the migration" mistake in CI. Case results store their
check outcomes as JSON text: the shape varies by evaluator and is read back whole, never queried
by field, so a JSON column is the honest choice over a wide table.

## Why the judge is pluggable and stubbed

`LlmJudgeEvaluator` depends on a `JudgeClient` interface. `JudgeConfig` binds the Gemini client
only when `GEMINI_API_KEY` is present in the environment; otherwise it binds a deterministic stub
that honours `expects:`/`rejects:` hints in the rubric. Everything else (parsing, scoring,
persistence, the API, CI) runs identically with either. The key is never in code or config files.

## Testing choices

Unit tests cover every evaluator, the factory, the scorer, the parser, the summary metrics, the diff
and the runner's concurrency bound (a test asserts the number of in-flight tasks never exceeds the
semaphore). The API is tested end to end against a real PostgreSQL in Testcontainers, so the
Flyway migration and JPA mappings are exercised, not mocked. JaCoCo measures line coverage; the
benchmark and the `main` class are excluded from the measurement because neither is logic.
