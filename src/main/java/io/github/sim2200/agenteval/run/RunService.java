package io.github.sim2200.agenteval.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sim2200.agenteval.domain.CaseResult;
import io.github.sim2200.agenteval.evaluator.JudgeClient;
import io.github.sim2200.agenteval.persistence.CaseResultEntity;
import io.github.sim2200.agenteval.persistence.Repositories.CaseResultRepository;
import io.github.sim2200.agenteval.persistence.Repositories.RunRepository;
import io.github.sim2200.agenteval.persistence.Repositories.SuiteRepository;
import io.github.sim2200.agenteval.persistence.RunEntity;
import io.github.sim2200.agenteval.persistence.SuiteEntity;
import io.github.sim2200.agenteval.scoring.Scorer;
import io.github.sim2200.agenteval.suite.SuiteParser;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use cases behind the API: store a suite, run it, read a run back, diff two runs. Execution is
 * synchronous: a run of 150 cases x 3 repeats with the stub judge takes well under a second, and
 * the endpoint returns the summary. A queue would be the first change at scale (see INTERVIEW.md).
 */
@Service
public class RunService {

  private final SuiteParser parser;
  private final CaseExecutor executor;
  private final JudgeClient judge;
  private final SuiteRepository suites;
  private final RunRepository runs;
  private final CaseResultRepository caseResults;
  private final ObjectMapper json = new ObjectMapper();

  public RunService(
      SuiteParser parser,
      CaseExecutor executor,
      JudgeClient judge,
      SuiteRepository suites,
      RunRepository runs,
      CaseResultRepository caseResults) {
    this.parser = parser;
    this.executor = executor;
    this.judge = judge;
    this.suites = suites;
    this.runs = runs;
    this.caseResults = caseResults;
  }

  @Transactional
  public SuiteEntity saveSuite(String yaml) {
    SuiteParser.Suite suite = parser.parse(yaml);
    return suites.save(new SuiteEntity(suite.id(), suite.name(), suite.cases().size(), yaml));
  }

  public List<SuiteEntity> listSuites() {
    return suites.findAll();
  }

  @Transactional
  public RunEntity run(String suiteId, int repeat, int concurrency) {
    SuiteEntity stored =
        suites
            .findById(suiteId)
            .orElseThrow(() -> new NoSuchElementException("no suite '" + suiteId + "'"));
    SuiteParser.Suite suite = parser.parse(stored.getYaml());
    RunEntity run = runs.save(new RunEntity(suiteId, repeat, concurrency, judge.name()));
    long start = System.nanoTime();
    try {
      List<CaseResult> results =
          ParallelRunner.run(suite.cases(), repeat, concurrency, executor::execute);
      long wall = (System.nanoTime() - start) / 1_000_000;
      RunSummary summary = RunSummary.of(results, new Scorer(repeat), wall);
      caseResults.saveAll(
          results.stream()
              .map(
                  r ->
                      new CaseResultEntity(
                          run.getId(), r.caseId(), r.repeatIndex(), r.passed(), toJson(r.checks())))
              .toList());
      run.finish(wall, toJson(summary));
    } catch (RuntimeException e) {
      run.fail();
      runs.save(run);
      throw e;
    }
    return runs.save(run);
  }

  public RunEntity getRun(long id) {
    return runs.findById(id).orElseThrow(() -> new NoSuchElementException("no run " + id));
  }

  public RunSummary summaryOf(RunEntity run) {
    if (run.getSummaryJson() == null) {
      throw new IllegalStateException(
          "run " + run.getId() + " has no summary (status " + run.getStatus() + ")");
    }
    try {
      return json.readValue(run.getSummaryJson(), RunSummary.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("stored summary is unreadable", e);
    }
  }

  public RunDiff diff(long baseId, long otherId) {
    RunEntity base = getRun(baseId);
    RunEntity other = getRun(otherId);
    if (!base.getSuiteId().equals(other.getSuiteId())) {
      throw new IllegalArgumentException("runs belong to different suites");
    }
    return RunDiff.of(baseId, summaryOf(base).caseScores(), otherId, summaryOf(other).caseScores());
  }

  private String toJson(Object o) {
    try {
      return json.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("cannot serialise " + o.getClass().getSimpleName(), e);
    }
  }
}
