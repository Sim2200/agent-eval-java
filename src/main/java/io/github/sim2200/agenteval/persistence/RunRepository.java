package io.github.sim2200.agenteval.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Runs; the query is derived from the method name. */
public interface RunRepository extends JpaRepository<RunEntity, Long> {
  List<RunEntity> findBySuiteIdOrderByStartedAtDesc(String suiteId);
}
