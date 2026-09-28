package io.github.sim2200.agenteval.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Suites by id. */
public interface SuiteRepository extends JpaRepository<SuiteEntity, String> {}
