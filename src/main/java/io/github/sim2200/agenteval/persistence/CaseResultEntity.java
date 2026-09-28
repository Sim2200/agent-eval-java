package io.github.sim2200.agenteval.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One (case, repeat) execution of a run, with its check outcomes as JSON. */
@Entity
@Table(name = "case_results")
public class CaseResultEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "run_id", nullable = false)
  private Long runId;

  @Column(name = "case_id", nullable = false)
  private String caseId;

  @Column(name = "repeat_index", nullable = false)
  private int repeatIndex;

  @Column(nullable = false)
  private boolean passed;

  @Column(name = "checks_json", nullable = false, columnDefinition = "text")
  private String checksJson;

  protected CaseResultEntity() {}

  public CaseResultEntity(
      Long runId, String caseId, int repeatIndex, boolean passed, String checksJson) {
    this.runId = runId;
    this.caseId = caseId;
    this.repeatIndex = repeatIndex;
    this.passed = passed;
    this.checksJson = checksJson;
  }

  public Long getId() {
    return id;
  }

  public Long getRunId() {
    return runId;
  }

  public String getCaseId() {
    return caseId;
  }

  public int getRepeatIndex() {
    return repeatIndex;
  }

  public boolean isPassed() {
    return passed;
  }

  public String getChecksJson() {
    return checksJson;
  }
}
