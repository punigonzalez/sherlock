-- Phase 2: fingerprints, incidents, evidence links, status history

CREATE TABLE error_fingerprints (
    fingerprint            TEXT PRIMARY KEY,
    normalized_signature   TEXT        NOT NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE incidents (
    id                 UUID PRIMARY KEY,
    fingerprint        TEXT        NOT NULL REFERENCES error_fingerprints (fingerprint),
    status             TEXT        NOT NULL,
    severity           TEXT        NOT NULL,
    title              TEXT        NOT NULL,
    first_seen_at      TIMESTAMPTZ NOT NULL,
    last_seen_at       TIMESTAMPTZ NOT NULL,
    occurrence_count   BIGINT      NOT NULL CHECK (occurrence_count >= 1),
    metadata           JSONB       NOT NULL DEFAULT '{}'::jsonb,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_incidents_fingerprint UNIQUE (fingerprint)
);

CREATE TABLE incident_evidence_links (
    id            UUID PRIMARY KEY,
    incident_id   UUID        NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    evidence_ref  TEXT        NOT NULL,
    linked_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_incident_evidence UNIQUE (incident_id, evidence_ref)
);

CREATE TABLE incident_status_history (
    id            BIGSERIAL PRIMARY KEY,
    incident_id   UUID        NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    from_status   TEXT,
    to_status     TEXT        NOT NULL,
    changed_at    TIMESTAMPTZ NOT NULL,
    reason        TEXT        NOT NULL DEFAULT ''
);

CREATE INDEX ix_incident_status_history_incident ON incident_status_history (incident_id);

UPDATE app_schema_meta SET schema_version = 'phase-2-incidents', applied_at = NOW() WHERE id = 1;
