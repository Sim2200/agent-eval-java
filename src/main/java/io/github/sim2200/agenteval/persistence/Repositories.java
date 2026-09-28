package io.github.sim2200.agenteval.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repositories; queries are derived from the method names. */
public final class Repositories {

  private Repositories() {}

  public interface SuiteRepository extends JpaRepository<SuiteEntity, String> {}

  public interface RunRepository extends JpaRepository<RunEntity, Long> {
    List<RunEntity> findBySuiteIdOrderByStartedAtDesc(String suiteId);
  }

  public interface CaseResultRepository extends JpaRepository<CaseResultEntity, Long> {
    List<CaseResultEntity> findByRunIdOrderByCaseIdAscRepeatIndexAsc(Long runId);
  }
}
