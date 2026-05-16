-- V1: Core schema for CI Failure Root-Cause Suggester
-- ===================================================

CREATE TABLE ci_job (
    id              UUID PRIMARY KEY,
    external_job_id VARCHAR(255)  NOT NULL,
    ci_platform     VARCHAR(20)   NOT NULL,
    project_name    VARCHAR(500)  NOT NULL,
    pipeline_ref    VARCHAR(255),
    branch_name     VARCHAR(500),
    commit_sha      VARCHAR(64),
    status          VARCHAR(20)   NOT NULL,
    started_at      TIMESTAMPTZ,
    finished_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_ci_job_external ON ci_job (external_job_id);
CREATE INDEX idx_ci_job_project  ON ci_job (project_name);
CREATE INDEX idx_ci_job_status   ON ci_job (status);

CREATE TABLE ci_job_log (
    id          UUID PRIMARY KEY,
    job_id      UUID          NOT NULL REFERENCES ci_job (id) ON DELETE CASCADE,
    log_source  VARCHAR(50)   NOT NULL DEFAULT 'console',
    log_content TEXT          NOT NULL,
    line_count  INTEGER,
    ingested_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_ci_job_log_job ON ci_job_log (job_id);

CREATE TABLE failure_pattern (
    id                        UUID PRIMARY KEY,
    name                      VARCHAR(255)  NOT NULL UNIQUE,
    failure_type              VARCHAR(20)   NOT NULL,
    regex_pattern             VARCHAR(2000) NOT NULL,
    explanation_template      VARCHAR(2000) NOT NULL,
    suggested_action_template VARCHAR(2000) NOT NULL,
    priority                  INTEGER       NOT NULL DEFAULT 0,
    active                    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_failure_pattern_type   ON failure_pattern (failure_type);
CREATE INDEX idx_failure_pattern_active ON failure_pattern (active);

CREATE TABLE model_version (
    id              UUID PRIMARY KEY,
    model_name      VARCHAR(255)  NOT NULL,
    version         VARCHAR(50)   NOT NULL,
    model_file_path VARCHAR(1000) NOT NULL,
    feature_schema  VARCHAR(4000),
    accuracy        DOUBLE PRECISION,
    active          BOOLEAN       NOT NULL DEFAULT FALSE,
    trained_at      TIMESTAMPTZ,
    deployed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_model_version_active ON model_version (active);

CREATE TABLE job_failure_analysis (
    id                 UUID PRIMARY KEY,
    job_id             UUID          NOT NULL REFERENCES ci_job (id) ON DELETE CASCADE,
    matched_pattern_id UUID          REFERENCES failure_pattern (id),
    model_version_id   UUID          REFERENCES model_version (id),
    failure_type       VARCHAR(20)   NOT NULL,
    confidence         DOUBLE PRECISION NOT NULL,
    explanation        VARCHAR(4000) NOT NULL,
    suggested_action   VARCHAR(4000) NOT NULL,
    classifier_mode    VARCHAR(20)   NOT NULL,
    matched_text       VARCHAR(2000),
    analyzed_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_analysis_job  ON job_failure_analysis (job_id);
CREATE INDEX idx_analysis_type ON job_failure_analysis (failure_type);
