# Log Detective (Sherlock)

Local modular-monolith MVP for log ingestion, deterministic detection/correlation, and optional AI investigation.

**Stack:** Java **25** + Spring Boot **4.1.1** · PostgreSQL 16 · Flyway · Maven Wrapper  

## Quick start (local)

```bash
# JDK 25 required (default shell JDK 21 will fail the release flag)
export JAVA_HOME=$(/usr/libexec/java_home -v 25)

cp .env.example .env
docker compose up -d postgres

./mvnw test
./mvnw spring-boot:run
# Health: http://127.0.0.1:8080/actuator/health
```

Bind defaults to **127.0.0.1**. File paths must stay under `logdetective.ingestion.allowlist-base-dir` (`./logs`).

## Docs

| Doc | Topic |
|-----|--------|
| `docs/architecture-phase-0.md` | Architecture / roadmap |
| `docs/api-cli.md` | REST + CLI |
| `docs/detection-correlation.md` | Threshold / spike / correlation |
| `docs/investigation-ai.md` | AI providers & tools |
| `docs/connectors.md` | File + GCP Logging |
| `docs/mvp-status.md` | Honesty checklist (what works / gaps) |
| `docs/security.md` | Security boundaries |

## Phases (local branches)

| Phase | Branch tip (local) |
|-------|-------------------|
| 5 API/CLI | `cursor/phase-5-api-cli-ed49` |
| 6 AI | `cursor/phase-6-ai-investigator-ed49` |
| 7 GCP Logging | `cursor/phase-7-gcp-logging-ed49` |
| 8 E2E | `cursor/phase-8-e2e-hardening-ed49` |

## Demo (seeded file → fake AI)

```bash
./scripts/demo-local.sh
# or: LOGDETECTIVE_INVESTIGATION_PROVIDER=fake ./mvnw -Dtest=LocalSeededEndToEndIT test
```

## Honest limits

- GCP Logging connector is **code-complete** but **not live-verified** here (needs ADC + project).
- OpenAI investigation needs `OPENAI_API_KEY`; default provider is `noop`.
- No Kafka / K8s / microservice split.
