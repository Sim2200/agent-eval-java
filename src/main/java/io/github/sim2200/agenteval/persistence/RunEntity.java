package io.github.sim2200.agenteval.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** One execution of a suite with its settings, status and the JSON summary once finished. */
@Entity
@Table(name = "runs")
public class RunEntity {

  public enum Status {
    RUNNING,
    FINISHED,
    FAILED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "suite_id", nullable = false)
  private String suiteId;

  @Column(nullable = false)
  private int repeat;

  @Column(nullable = false)
  private int concurrency;

  @Column(nullable = false)
  private String judge;

  @Column(nullable = false)
  private String status = Status.RUNNING.name();

  @Column(name = "started_at", nullable = false)
  private OffsetDateTime startedAt = OffsetDateTime.now();

  @Column(name = "finished_at")
  private OffsetDateTime finishedAt;

  @Column(name = "wall_millis")
  private Long wallMillis;

  @Column(name = "summary_json", columnDefinition = "text")
  private String summaryJson;

  protected RunEntity() {}

  public RunEntity(String suiteId, int repeat, int concurrency, String judge) {
    this.suiteId = suiteId;
    this.repeat = repeat;
    this.concurrency = concurrency;
    this.judge = judge;
  }

  public void finish(long wallMillis, String summaryJson) {
    this.status = Status.FINISHED.name();
    this.finishedAt = OffsetDateTime.now();
    this.wallMillis = wallMillis;
    this.summaryJson = summaryJson;
  }

  public void fail() {
    this.status = Status.FAILED.name();
    this.finishedAt = OffsetDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public String getSuiteId() {
    return suiteId;
  }

  public int getRepeat() {
    return repeat;
  }

  public int getConcurrency() {
    return concurrency;
  }

  public String getJudge() {
    return judge;
  }

  public String getStatus() {
    return status;
  }

  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public OffsetDateTime getFinishedAt() {
    return finishedAt;
  }

  public Long getWallMillis() {
    return wallMillis;
  }

  public String getSummaryJson() {
    return summaryJson;
  }
}
