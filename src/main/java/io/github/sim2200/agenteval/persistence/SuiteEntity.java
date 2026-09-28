package io.github.sim2200.agenteval.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** A stored suite: the YAML as uploaded, plus what the parser found in it. */
@Entity
@Table(name = "suites")
public class SuiteEntity {

  @Id private String id;

  @Column(nullable = false)
  private String name;

  @Column(name = "case_count", nullable = false)
  private int caseCount;

  @Column(nullable = false, columnDefinition = "text")
  private String yaml;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  protected SuiteEntity() {}

  public SuiteEntity(String id, String name, int caseCount, String yaml) {
    this.id = id;
    this.name = name;
    this.caseCount = caseCount;
    this.yaml = yaml;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getCaseCount() {
    return caseCount;
  }

  public String getYaml() {
    return yaml;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
