-- Phase 4: evidence records + richer incident evidence links

CREATE TABLE evidence (
    id           UUID PRIMARY KEY,
    kind         TEXT        NOT NULL,
    strength     TEXT        NOT NULL,
    reason       TEXT        NOT NULL,
    summary      TEXT        NOT NULL,
    attributes   JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE evidence_event_links (
    evidence_id UUID NOT NULL REFERENCES evidence (id) ON DELETE CASCADE,
    event_id    UUID NOT NULL,
    PRIMARY KEY (evidence_id, event_id)
);

CREATE INDEX ix_evidence_event_links_event ON evidence_event_links (event_id);

ALTER TABLE incident_evidence_links
    ADD COLUMN strength TEXT,
    ADD COLUMN reason TEXT;

UPDATE app_schema_meta SET schema_version = 'phase-4-detection', applied_at = NOW() WHERE id = 1;
