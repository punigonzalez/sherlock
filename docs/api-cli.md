# REST API & CLI (Phase 5)

Local MVP surface over application use cases. Controllers/CLI never call persistence adapters directly.

**Defaults:** `server.address=127.0.0.1`, file paths must resolve under `logdetective.ingestion.allowlist-base-dir` (default `./logs`).

## Health

| Endpoint | Notes |
|----------|--------|
| `GET /actuator/health` | Spring Actuator readiness/liveness |
| `GET /actuator/info` | Exposed with health |

## REST (`/api/v1`)

### Sources

| Method | Path | Body / params |
|--------|------|----------------|
| `GET` | `/sources` | List running file sources + health |
| `POST` | `/sources/validate` | `{ "id", "path", "encoding?" }` |
| `POST` | `/sources` | Register + start (201) |
| `GET` | `/sources/{id}/health` | Single source health |
| `POST` | `/sources/{id}/ingest` | One-shot ingest batch |

### Incidents & evidence

| Method | Path | Params |
|--------|------|--------|
| `GET` | `/incidents` | `page`, `size` (max 100), `status` |
| `GET` | `/incidents/{id}` | Detail DTO (not JPA entity) |
| `GET` | `/incidents/{id}/evidence` | Linked evidence list |

### Investigations (honest NoOp)

| Method | Path | Notes |
|--------|------|--------|
| `POST` | `/incidents/{id}/investigations` | Creates report; `aiUnavailable=true` with NoOp AI |
| `GET` | `/incidents/{id}/investigations/latest` | 404 if none yet |

Errors use `ApiError`: `timestamp`, `status`, `error`, `message`, `path`, `details[]`.

## CLI

Activate with `--cli=<command>` (process exits after the command):

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--cli=help"

# Examples
--cli=validate-source --id=app --path=./logs/app.log
--cli=register-source --id=app --path=app.log
--cli=list-sources
--cli=source-health --id=app
--cli=ingest --id=app
--cli=list-incidents --page=0 --size=20 --status=OPEN
--cli=show-incident --id=<uuid>
--cli=list-evidence --id=<uuid>
--cli=request-investigation --id=<uuid>
--cli=show-investigation --id=<uuid>
```

## Config / env

| Property / env | Default | Purpose |
|----------------|---------|---------|
| `SERVER_ADDRESS` | `127.0.0.1` | Bind address |
| `SERVER_PORT` | `8080` | HTTP port |
| `LOGDETECTIVE_ALLOWLIST_BASE_DIR` | `./logs` | Path allowlist root |
| `LOGDETECTIVE_INGESTION_ENABLED` | `false` | Auto-start configured sources + poller |

## Out of scope (later phases)

- Real AI diagnosis (Phase 6)
- GCP log connector (Phase 7)
- OpenAPI UI (optional; DTOs are Jackson-friendly)
