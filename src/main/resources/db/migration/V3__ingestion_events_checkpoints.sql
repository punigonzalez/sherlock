-- Phase 3: canonical events + source checkpoints for restart recovery

CREATE TABLE canonical_events (
    id                  UUID PRIMARY KEY,
    source_id           TEXT        NOT NULL,
    source_payload_id   TEXT        NOT NULL,
    event_timestamp     TIMESTAMPTZ NOT NULL,
    ingested_at         TIMESTAMPTZ NOT NULL,
    level               TEXT        NOT NULL,
    message             TEXT        NOT NULL,
    logger              TEXT,
    service             TEXT,
    host                TEXT,
    trace_id            TEXT,
    span_id             TEXT,
    attributes          JSONB       NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT ux_canonical_events_source_payload UNIQUE (source_id, source_payload_id)
);

CREATE INDEX ix_canonical_events_source_time ON canonical_events (source_id, event_timestamp);

CREATE TABLE source_checkpoints (
    source_id     TEXT PRIMARY KEY,
    cursor_opaque TEXT        NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

UPDATE app_schema_meta SET schema_version = 'phase-3-ingestion', applied_at = NOW() WHERE id = 1;
