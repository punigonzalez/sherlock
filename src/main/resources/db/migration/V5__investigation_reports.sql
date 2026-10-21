-- Phase 6: persisted investigation / diagnostic reports

CREATE TABLE investigation_reports (
    id                      UUID PRIMARY KEY,
    incident_id             UUID        NOT NULL REFERENCES incidents (id),
    status                  TEXT        NOT NULL,
    provider                TEXT        NOT NULL,
    summary                 TEXT        NOT NULL,
    hypothesis              TEXT,
    confidence              TEXT        NOT NULL DEFAULT 'NONE',
    evidence_citation_ids   JSONB       NOT NULL DEFAULT '[]'::jsonb,
    tool_calls              JSONB       NOT NULL DEFAULT '[]'::jsonb,
    metadata                JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_investigation_reports_incident_created
    ON investigation_reports (incident_id, created_at DESC);

UPDATE app_schema_meta SET schema_version = 'phase-6-investigation', applied_at = NOW() WHERE id = 1;
