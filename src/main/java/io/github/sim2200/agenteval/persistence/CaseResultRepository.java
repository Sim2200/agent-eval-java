package io.github.sim2200.agenteval.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Per-execution case results of a run. */
public interface CaseResultRepository extends JpaRepository<CaseResultEntity, Long> {
  List<CaseResultEntity> findByRunIdOrderByCaseIdAscRepeatIndexAsc(Long runId);
}
