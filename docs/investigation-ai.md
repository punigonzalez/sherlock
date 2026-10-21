# Investigation / AI (Phase 6)

Provider-neutral investigation with bounded read-only tools. Deterministic detection works when AI is `noop` or unavailable.

## Providers

| `logdetective.investigation.provider` | Behavior |
|--------------------------------------|----------|
| `noop` (default) | Honest `NOOP_UNAVAILABLE` report; no LLM |
| `fake` | Deterministic tool walk; structured report; for tests/E2E |
| `openai` | Spring AI 2.0.x `ChatModel` + tool-bounded loop (requires `OPENAI_API_KEY`) |

Spring AI **2.0.1** supports Boot **4.1.x**. The OpenAI bean is conditional; builds never need a key.

## Tools (read-only)

| Tool | Notes |
|------|--------|
| `getIncidentDetails` | Incident fields |
| `getRelatedErrors` | Linked evidence + fingerprint |
| `getRecentEvents` | Last hour of events |
| `searchLogs` | Substring over last 6h |
| `getTraceDetails` | Events by `traceId` |
| `getServiceMetrics` | Explicit `UNSUPPORTED` unless raw count exists |

Limits: `max-tool-calls`, `timeout-seconds`, `max-log-chars`, `tool-row-limit`.  
Log text is redacted and wrapped as `<UNTRUSTED_LOG>` (prompt-injection mitigation).

## Persistence

Flyway `V5__investigation_reports.sql` — reports + tool call metadata stored in PostgreSQL.

## Env

```bash
LOGDETECTIVE_INVESTIGATION_PROVIDER=fake   # or noop|openai
OPENAI_API_KEY=...                         # only for openai
OPENAI_MODEL=gpt-4o-mini
```
