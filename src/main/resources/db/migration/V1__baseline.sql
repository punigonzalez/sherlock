-- Phase 1 baseline: schema marker only. Domain tables arrive in later phases.
CREATE TABLE app_schema_meta (
    id              SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    schema_version  TEXT        NOT NULL,
    applied_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO app_schema_meta (schema_version) VALUES ('phase-1-baseline');
