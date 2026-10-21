# Security boundaries (MVP)

## Network

- Default `server.address=127.0.0.1` — not exposed on LAN unless overridden.
- No authentication layer yet — **do not** bind to `0.0.0.0` in shared environments.

## Filesystem

- File sources/API/CLI paths must resolve under `logdetective.ingestion.allowlist-base-dir`.
- `SafePathValidator` rejects traversal outside the allowlist.

## Secrets

- Datasource password, OpenAI key, GCP ADC via env / ADC files — never committed.
- Ingest redacts common email / bearer / api-key patterns (`BasicRedactionHook`).
- Investigation wraps log text as `<UNTRUSTED_LOG>` and filters instruction-like markers.

## AI

- Default `noop` — no external calls.
- `fake` — local tools only.
- `openai` — requires explicit provider + API key; tool calls are bounded (count, timeout, row/char limits).

## GCP

- Opt-in (`logdetective.gcp-logging.enabled=false` by default).
- Uses ADC; minimum IAM documented in `docs/connectors.md`.
- Pull-only (no Pub/Sub push surface).

## Actuator

- Exposes `health` and `info` only. Health details: `when_authorized`.
