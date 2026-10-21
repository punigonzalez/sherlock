# Connectors

## Local file (Phase 3)

See ingestion config under `logdetective.ingestion.*` and `docs/api-cli.md`.

## Google Cloud Logging (Phase 7)

Official `google-cloud-logging` client with **Application Default Credentials (ADC)**.  
**No Pub/Sub** — pull via `listLogEntries` with filter, pagination, and checkpoint watermarks.

### Enable

```bash
export LOGDETECTIVE_GCP_LOGGING_ENABLED=true
export GCP_PROJECT_ID=my-project
export LOGDETECTIVE_GCP_FILTER='severity>=ERROR'
export LOGDETECTIVE_GCP_HISTORICAL_LOOKBACK=PT1H
# ADC: gcloud auth application-default login   OR   GOOGLE_APPLICATION_CREDENTIALS=/path/key.json
```

### Behavior

| Aspect | Detail |
|--------|--------|
| Historical | First pull bounded by `historical-lookback` |
| Pagination | `page-size` + `max-pages-per-poll` per scheduler tick |
| Checkpoint | Opaque cursor `ts=<ISO>\|token=` in `source_checkpoints` |
| Health | `GcpLoggingSourceRegistry.health()` / source `SourceHealth` |
| Live IT | `LOGDETECTIVE_GCP_IT=true` + ADC (skipped in default CI) |

### IAM minimum permissions

Grant the runtime principal (user account / ADC user) on the project:

| Role or permission | Why |
|--------------------|-----|
| `roles/logging.viewer` | List log entries and log names |
| *or* finer: `logging.logEntries.list` + `logging.logs.list` | Same capability without broader viewer |

Optional if reading across folders/org: additional Logging Viewer on those resources.  
**Do not** grant `logging.admin` or write roles for this connector.

### Out of scope

- Pub/Sub push subscriptions
- Cross-project sinks without explicit IAM
- Live GCP verification in default CI
