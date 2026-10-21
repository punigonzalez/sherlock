# Log Detective — Phase 0 Architecture

**Status:** awaiting user validation before Phase 1  
**Repo inspected:** `/Users/punigonzalez/Documents/sherlock`  
**Date:** 2026-10-20  
**Stack decision (user-approved):** Java **25** + Spring Boot **4.1.1** (preserve Initializr scaffold)

---

## 0. Inspection summary

| Item | Finding |
|------|---------|
| Scaffold | Spring Initializr Maven project `com.sherlock:sherlock:0.0.1-SNAPSHOT` |
| Parent BOM | `spring-boot-starter-parent` **4.1.1** (**keep**) |
| `pom.xml` Java | `<java.version>25</java.version>` (**keep**) |
| Host JDKs | Default shell often **21**; **JDK 25** available (Corretto / Homebrew / Oracle) |
| Maven Wrapper | 3.10.0 (`./mvnw`) |
| App entry | `com.sherlock.sherlock.SherlockApplication` |
| Config | `src/main/resources/application.properties` → `spring.application.name=sherlock` only |
| Dependencies | `spring-boot-starter` + `spring-boot-starter-test` only |
| Docker / Compose | **none** in repo; Docker **29.4.3** + Compose **v2.40.3** available on host |
| Flyway / PostgreSQL / Web | **not present** |
| Build with JDK 21 (default PATH) | `./mvnw test` **fails**: `release version 25 not supported` |
| Build with JDK 25 (`JAVA_HOME` → Corretto 25) | `./mvnw test` **PASS** (Boot 4.1.1, Java 25.0.2) |

**Decision:** stay on the Initializr stack — **Spring Boot 4.1.1 + Java 25**. Do not downgrade to Boot 3.x / Java 21. Phase 1 must document/use JDK 25 (`JAVA_HOME`) so Maven does not pick JDK 21 by accident.

---

## 1. Architecture proposal

**Style:** modular monolith + Hexagonal Architecture (ports & adapters), package-by-feature.

```
                    ┌─────────────────────────────────────┐
                    │         Adapters (in)               │
                    │  REST / CLI ingest / File watcher   │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │         Application (use cases)     │
                    │  Ingest · Detect · Correlate · AI   │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │         Domain (pure)               │
                    │  Event · Incident · Evidence · Rule │
                    └──────────────┬──────────────────────┘
                                   │ ports
          ┌────────────────────────┼────────────────────────┐
          ▼                        ▼                        ▼
   Persistence              LogSource                 AiInvestigator
   (JPA/Flyway)             (adapters out)            (optional port)
          ▼                        ▼                        ▼
   PostgreSQL              File / HTTP / stub         GCP / Fake / NoOp
```

**Principles**
- Domain has **no** Spring/JPA/HTTP dependencies.
- Deterministic detection & correlation run **without** any AI provider (`AiInvestigator` = optional; default `NoOp` / `Fake`).
- One deployable JAR; modules are packages, not separate services.
- Out of MVP: Kafka, Kubernetes, microservice split.

---

## 2. Package / directory structure

Keep Initializr root package `com.sherlock.sherlock` as the Spring Boot entry; grow feature modules under `com.sherlock.logdetective` (or rename later — Phase 1 keeps entry class to avoid breaking scaffold).

```
src/main/java/com/sherlock/
  sherlock/
    SherlockApplication.java                 # keep Initializr entry
  logdetective/
    shared/
      domain/                                # shared value objects (Id, TimeRange)
      config/                                # Spring @Configuration (adapters wiring)
    ingestion/
      domain/          # CanonicalEvent, LogSourceId
      application/     # IngestLogsUseCase
      port/in/         # IngestLogsCommand / Port
      port/out/        # EventRepository, LogSource
      adapter/in/      # RestIngestController, FileIngestRunner
      adapter/out/     # JpaEventRepository, FileLogSource, HttpLogSource
    detection/
      domain/          # Rule, DetectionMatch, IncidentCandidate
      application/     # DetectIncidentsUseCase
      port/in/
      port/out/        # RuleRepository, IncidentRepository
      adapter/in/      # DetectionTrigger (scheduled / after-ingest)
      adapter/out/     # InMemoryRuleEngine, JpaIncidentRepository
    correlation/
      domain/          # Evidence, EvidenceLink
      application/     # CorrelateEvidenceUseCase
      port/...
      adapter/...
    investigation/
      domain/          # InvestigationReport (evidence citations required)
      application/     # InvestigateIncidentUseCase
      port/out/        # AiInvestigator
      adapter/out/     # NoOpAiInvestigator, FakeAiInvestigator, GcpAiInvestigator
    api/               # cross-cutting DTOs / OpenAPI if needed

src/main/resources/
  application.yml                            # replace/extend .properties in Phase 1
  application-local.yml
  db/migration/                              # Flyway V1__...
  rules/                                     # optional YAML rule packs

src/test/java/com/sherlock/logdetective/
  .../unit/
  .../integration/                           # Testcontainers
  .../fakes/

docker-compose.yml                           # PostgreSQL only (MVP)
Dockerfile                                   # optional thin image later (FROM eclipse-temurin:25)
```

**Config note:** today only `application.properties` exists. Phase 1 migrates to `application.yml` (or keeps properties — either is fine; YAML preferred for multi-profile Docker Compose).

---

## 3. Principal domain entities and relationships

```
LogSourceDescriptor 1 ── * CanonicalEvent
CanonicalEvent * ── * DetectionMatch (via rule evaluation)
DetectionMatch * ── 1 Incident
Incident 1 ── * Evidence
Evidence * ── * CanonicalEvent          (evidence ↔ event links)
Incident 0..1 ── 1 InvestigationReport  (optional AI path)
InvestigationReport 1 ── * EvidenceCitation (must reference Evidence ids)
```

| Entity | Responsibility |
|--------|----------------|
| **LogSourceDescriptor** | Registered source (id, type, uri/path, enabled) |
| **CanonicalEvent** | Normalized log line/record after ingest |
| **Rule** | Deterministic matcher (pattern/threshold/window) |
| **DetectionMatch** | Rule hit with severity + matched event ids |
| **Incident** | Aggregated incident (status, severity, window) |
| **Evidence** | Correlated facts supporting the incident |
| **InvestigationReport** | AI or template narrative **grounded** in Evidence ids |

**Relationships (cardinality)**
- One source produces many events.
- Many matches fold into one Incident (dedupe by fingerprint: ruleId + entity keys + time bucket).
- Evidence always points back to events; AI must not invent event ids.

---

## 4. `LogSource` contract and canonical event schema

### Port: `LogSource`

```java
public interface LogSource {
  LogSourceId id();
  SourceType type(); // FILE, HTTP, STUB
  /** Pull batch since cursor; empty = no more for now. */
  List<RawLogRecord> fetch(FetchCursor cursor, int limit);
  FetchCursor advance(FetchCursor cursor, List<RawLogRecord> consumed);
}
```

```java
public record RawLogRecord(
  String sourcePayloadId,   // opaque id from source (line offset, HTTP id)
  Instant observedAt,       // best-effort from payload or ingest time
  String rawBody,
  Map<String, String> headers // optional transport metadata
) {}

public record FetchCursor(String opaque) {} // e.g. file byte offset / page token
```

### Canonical event schema (persisted)

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | Surrogate PK |
| `sourceId` | String | LogSource id |
| `sourcePayloadId` | String | Idempotency with sourceId |
| `timestamp` | Instant | Event time (UTC) |
| `ingestedAt` | Instant | Ingest wall clock |
| `level` | enum | TRACE…ERROR / UNKNOWN |
| `message` | String | Primary text |
| `logger` | String? | Optional |
| `service` | String? | Optional service/app name |
| `host` | String? | Optional |
| `traceId` / `spanId` | String? | Correlation keys |
| `attributes` | JSONB | Remaining structured fields |
| `fingerprint` | String | Hash for dedupe (sourceId+sourcePayloadId) |

**Idempotency:** unique constraint `(source_id, source_payload_id)`.

**Normalization:** adapters map raw → canonical; unknown fields land in `attributes`. Detection reads only canonical fields + documented attribute keys.

---

## 5. Dependency list (pinned) and compatibility

**Target stack (user-approved):** Java **25**, Spring Boot **4.1.1** (Initializr parent as-is). Boot 4.1.1 officially supports Java 17–26 inclusive.

| Dependency | Version strategy | Why |
|------------|------------------|-----|
| `spring-boot-starter-parent` | **4.1.1** | Keep scaffold; BOM manages plugins/deps |
| `java.version` | **25** | Keep scaffold; matches available JDKs on Mac |
| `spring-boot-starter-web` | managed by 4.1.1 | REST ingest / query API |
| `spring-boot-starter-data-jpa` | managed | Persistence adapter |
| `postgresql` | managed | JDBC driver |
| `flyway-core` (+ DB module if BOM requires) | managed | Schema migrations |
| `spring-boot-starter-validation` | managed | Request/command validation |
| `spring-boot-starter-actuator` | managed (optional Phase 2+) | Health for Compose |
| `spring-boot-starter-test` | already present | JUnit 5, Mockito, AssertJ |
| `spring-boot-testcontainers` | managed | `@ServiceConnection` style IT |
| `testcontainers` + `postgresql` module | BOM via Boot | Integration tests |
| Jackson | managed | JSONB / API |
| AI client (GCP) | **optional**, behind port; add only in investigation phase | Deterministic path must not depend on it |

**Phase 1 pom changes (add deps only — do not change Boot/Java):**
1. Keep parent `4.1.1` and `java.version` `25`
2. Add web, data-jpa, postgresql, flyway, validation
3. Keep `SherlockApplication` package
4. Ensure builds run with JDK 25 (`JAVA_HOME`)

**Not in MVP:** Kafka, Spring Cloud, Redis, Elasticsearch, K8s operators.

---

## 6. Testing strategy

| Layer | Approach |
|-------|----------|
| **Unit** | Domain + use cases with in-memory fakes for ports; no Spring context |
| **Adapter unit** | Controllers with `@WebMvcTest`; JPA mapping tests optional |
| **Integration** | Testcontainers PostgreSQL + Flyway; full ingest → detect happy path |
| **AI** | `FakeAiInvestigator` returns fixed report citing provided Evidence ids; contract test asserts no hallucinated evidence ids |
| **Determinism gate** | CI profile with AI bean = `NoOp`; suite must be green |
| **Optional GCP** | Manual / tagged `@EnabledIfEnvironmentVariable`; never required for merge |
| **Smoke** | Existing `contextLoads` already green on JDK 25 |

**Test layout:** mirror main packages under `src/test/java`; shared fakes in `logdetective/testing/fakes`.

**Toolchain note:** Mockito may warn about dynamic agent loading on JDK 25; Phase 1 can add the Mockito agent config from Boot/Mockito docs if warnings become failures.

---

## 7. Security risks and mitigations

| Risk | Mitigation |
|------|------------|
| Secrets in repo / config | Env vars / `application-local.yml` (gitignored); never commit API keys |
| Log injection / XSS in UI later | Escape on render; store raw separately from display |
| Path traversal in `FileLogSource` | Resolve under allowlisted base dir; reject `..` |
| SSRF via `HttpLogSource` | Allowlist hosts; block link-local/metadata IPs |
| Prompt injection via log text → AI | Treat logs as untrusted data; constrain tools; require evidence-id citations |
| Overly broad DB access | App role with least privilege; no superuser in Compose for app user |
| Dependency CVEs | Stay on Boot-managed BOM; Dependabot/manual review later |
| PII in logs | Redaction hook at ingest (Phase 4+); document retention |

---

## 8. Incremental plan (Phases 1–8)

| Phase | Goal |
|-------|------|
| **1** | Keep Boot 4.1.1 / Java 25; Docker Compose PostgreSQL; Flyway baseline; hexagonal skeleton; health/`contextLoads` green under JDK 25 |
| **2** | Domain model + error fingerprints + incident lifecycle + JPA persistence (grouping by fingerprint). **Done** |
| **3** | File ingestion pipeline (`LogSource` + File source; parsers; checkpointing; wire errors to incidents). **Done** |
| **4** | Threshold/spike detection + trace/temporal correlation + evidence links. **Done** — see `internal/phase-4-completion.md` |
| **5** | Query API / REST-CLI polish: incidents, events, evidence |
| **6** | Optional real AI provider (NoOp already present); optional GCP adapter |
| **7** | Hardening: validation, redaction basics, Dockerfile (Java 25 base) |
| **8** | Polish: sample datasets, docs, demo script; quality gate (coverage on domain) |

> **Note:** Phase numbering in active briefs supersedes the original Phase 0 draft where Phase 2 was ingest.

---

## 9. Acceptance criteria for Phase 1

Phase 1 is done when **all** of the following hold:

1. `pom.xml` keeps **Java 25** and Spring Boot **4.1.1**.
2. `./mvnw test` passes with **JDK 25** (`JAVA_HOME` pointing at a Java 25 install).
3. `docker compose up -d` starts PostgreSQL; app connects via env-based datasource (no hardcoded passwords in source).
4. Flyway applies a baseline migration successfully on empty DB.
5. Package skeleton for `ingestion`, `detection`, `correlation`, `investigation` exists with empty ports/adapters as needed (no full business logic required beyond wiring).
6. Hexagonal boundary documented in code: domain packages compile without Spring imports (spot-check).
7. `AiInvestigator` port present with `NoOp` implementation; app starts without AI credentials.
8. Repo remains local-only (no remote push required).

---

## Commands run (Phase 0)

```text
# Default PATH (often JDK 21)
java -version
# openjdk 21.0.10

./mvnw -q test
# FAIL: release version 25 not supported

# With JDK 25
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-25.jdk/Contents/Home
java -version
# openjdk 25.0.2 (Corretto)

./mvnw -q test
# PASS — Spring Boot 4.1.1

docker --version && docker compose version
# Docker 29.4.3 / Compose v2.40.3-desktop.1

/usr/libexec/java_home -V
# 25.0.2 Corretto, 25.0.2 Homebrew, 25.0.1 Oracle, 21.0.10 Homebrew
```

## Known limitations

- Shell default may still be JDK 21; Phase 1 tooling/docs must set `JAVA_HOME` to 25.
- No product UI/UX scope in this plan.
- Exact GCP client library deferred until Phase 6.
- Artifact/name still `sherlock`; product name Log Detective can be reflected in docs/`spring.application.name` later.
- Original Phase 0 brief mentioned Boot 3.x / Java 21; **superseded by user decision** (Boot 4.1.1 / Java 25).

## Phase 1 readiness

**Ready to start Phase 1 after user validates** this revised plan. Stack is locked to **Java 25 + Spring Boot 4.1.1**. No Phase 1 work has been started.
