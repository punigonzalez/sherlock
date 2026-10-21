# MVP status (Phase 8 honesty)

## Met

| Criterion | Evidence |
|-----------|----------|
| Modular monolith + hexagonal packages | `com.sherlock.logdetective.*` |
| PostgreSQL + Flyway through V5 | IT schema `phase-6-investigation` |
| File ingest + checkpoints + restart-safe re-read | `FileIngestionIntegrationTest` |
| Fingerprint incidents (no unbounded dupes on re-ingest) | `LocalSeededEndToEndIT` |
| Threshold/spike + trace/temporal evidence | Phase 4 ITs |
| REST + CLI over use cases | `docs/api-cli.md` |
| Localhost bind + path allowlist | `application.yml` / `SafePathValidator` |
| AI noop/fake + optional OpenAI (Spring AI 2.0.1) | `docs/investigation-ai.md` |
| Persisted investigation reports | V5 + E2E |
| GCP Logging connector (code) | `docs/connectors.md` |
| Docker Compose Postgres | `docker compose up -d postgres` exercised |
| Dockerfile (Java 25) | root `Dockerfile` |

## Not met / not verified

| Gap | Notes |
|-----|--------|
| Live GCP Logging against a real project | Requires ADC + `GCP_PROJECT_ID`; IT skipped unless `LOGDETECTIVE_GCP_IT=true` |
| Live OpenAI diagnosis | Needs `OPENAI_API_KEY`; not exercised in CI |
| AuthN/AuthZ for API | Intentionally out of local MVP |
| OpenAPI UI | DTOs only |
| Production hardening (rate limits, TLS, secrets manager) | Out of scope |
| Coverage gate / quality metrics dashboard | Not enforced |

## Compose exercise (Phase 8)

Performed on this machine:

```text
docker compose up -d postgres
# status: healthy on host port (mapped)
docker compose exec postgres pg_isready ... → accepting connections
```

App datasource should use the same credentials as `.env` / Compose.
