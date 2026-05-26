-- V3: Performance indexes + API key audit table
-- Applied automatically by Flyway on startup.

-- ── Additional indexes for history pagination ──────────────────────────────────
-- Supports  ORDER BY analyzed_at DESC  queries efficiently
CREATE INDEX IF NOT EXISTS idx_analysis_analyzed_at
    ON job_failure_analysis (analyzed_at DESC);

-- Supports failure-type filter + analyzed_at sort
CREATE INDEX IF NOT EXISTS idx_analysis_type_analyzed_at
    ON job_failure_analysis (failure_type, analyzed_at DESC);

-- Supports project summary queries
CREATE INDEX IF NOT EXISTS idx_ci_job_project_created
    ON ci_job (project_name, created_at DESC);

-- ── API key audit log ──────────────────────────────────────────────────────────
-- Tracks which API key was used for each analysis request (optional, set via filter)
CREATE TABLE IF NOT EXISTS api_key_audit (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    key_prefix  VARCHAR(12) NOT NULL,       -- first 12 chars of the key (safe to log)
    endpoint    VARCHAR(200),
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500),
    status_code INT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_api_audit_prefix
    ON api_key_audit (key_prefix, requested_at DESC);

COMMENT ON TABLE api_key_audit IS
    'Lightweight audit trail for API key usage. key_prefix stores only the first 12 chars of the actual key.';
