-- Suites are stored as the YAML that was uploaded; runs store their summary and every case result.
CREATE TABLE suites (
    id          VARCHAR(120) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    case_count  INTEGER      NOT NULL,
    yaml        TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE runs (
    id             BIGSERIAL    PRIMARY KEY,
    suite_id       VARCHAR(120) NOT NULL REFERENCES suites (id),
    repeat         INTEGER      NOT NULL,
    concurrency    INTEGER      NOT NULL,
    judge          VARCHAR(40)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    started_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at    TIMESTAMPTZ,
    wall_millis    BIGINT,
    summary_json   TEXT
);

CREATE INDEX runs_suite_idx ON runs (suite_id, started_at DESC);

CREATE TABLE case_results (
    id            BIGSERIAL    PRIMARY KEY,
    run_id        BIGINT       NOT NULL REFERENCES runs (id) ON DELETE CASCADE,
    case_id       VARCHAR(120) NOT NULL,
    repeat_index  INTEGER      NOT NULL,
    passed        BOOLEAN      NOT NULL,
    checks_json   TEXT         NOT NULL
);

CREATE INDEX case_results_run_idx ON case_results (run_id, case_id);
