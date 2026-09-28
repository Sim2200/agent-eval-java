package io.github.sim2200.agenteval.api;

import io.github.sim2200.agenteval.persistence.RunEntity;
import io.github.sim2200.agenteval.persistence.SuiteEntity;
import io.github.sim2200.agenteval.run.RunDiff;
import io.github.sim2200.agenteval.run.RunService;
import io.github.sim2200.agenteval.run.RunSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The REST surface. Suites are posted as YAML text; everything else is JSON. */
@RestController
public class ApiController {

  private final RunService service;

  public ApiController(RunService service) {
    this.service = service;
  }

  public record SuiteView(String id, String name, int caseCount) {
    static SuiteView of(SuiteEntity e) {
      return new SuiteView(e.getId(), e.getName(), e.getCaseCount());
    }
  }

  public record RunRequest(
      @NotBlank String suiteId,
      @Min(1) @Max(10) Integer repeat,
      @Min(1) @Max(1000) Integer concurrency) {}

  public record RunView(
      long id,
      String suiteId,
      String status,
      int repeat,
      int concurrency,
      String judge,
      RunSummary summary) {
    static RunView of(RunEntity r, RunSummary s) {
      return new RunView(
          r.getId(),
          r.getSuiteId(),
          r.getStatus(),
          r.getRepeat(),
          r.getConcurrency(),
          r.getJudge(),
          s);
    }
  }

  @PostMapping(
      value = "/suites",
      consumes = {
        "application/x-yaml",
        "text/yaml",
        "text/plain",
        MediaType.APPLICATION_OCTET_STREAM_VALUE
      })
  @ResponseStatus(HttpStatus.CREATED)
  public SuiteView createSuite(@RequestBody String yaml) {
    return SuiteView.of(service.saveSuite(yaml));
  }

  @GetMapping("/suites")
  public List<SuiteView> listSuites() {
    return service.listSuites().stream().map(SuiteView::of).toList();
  }

  @PostMapping("/runs")
  @ResponseStatus(HttpStatus.CREATED)
  public RunView createRun(@Valid @RequestBody RunRequest request) {
    int repeat = request.repeat() == null ? 3 : request.repeat();
    int concurrency = request.concurrency() == null ? 32 : request.concurrency();
    RunEntity run = service.run(request.suiteId(), repeat, concurrency);
    return RunView.of(run, service.summaryOf(run));
  }

  @GetMapping("/runs/{id}")
  public RunView getRun(@PathVariable long id) {
    RunEntity run = service.getRun(id);
    return RunView.of(run, run.getSummaryJson() == null ? null : service.summaryOf(run));
  }

  @GetMapping("/runs/{id}/diff/{otherId}")
  public RunDiff diff(@PathVariable long id, @PathVariable long otherId) {
    return service.diff(id, otherId);
  }
}
