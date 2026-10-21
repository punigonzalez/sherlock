# Incident lifecycle

States: `OPEN` → `INVESTIGATING` → `RESOLVED` → `REOPENED` (and back).

```
OPEN ──────────────► INVESTIGATING ──────────────► RESOLVED
 ▲                        │                           │
 │                        ▼                           │
 └────────────── REOPENED ◄───────────────────────────┘
```

## Allowed transitions

| From | To |
|------|----|
| OPEN | INVESTIGATING, RESOLVED |
| INVESTIGATING | OPEN, RESOLVED |
| RESOLVED | REOPENED |
| REOPENED | INVESTIGATING, RESOLVED, OPEN |

## Observation rules

- New observation with an unseen fingerprint → create incident `OPEN` (count = 1).
- Observation matching an existing fingerprint → increment count, update `lastSeen`, attach evidence ref if present.
- Observation on a `RESOLVED` incident → automatic transition to `REOPENED`.

Grouping key: deterministic `ErrorFingerprint` (SHA-256 of normalized signature: level + message + optional logger/service after volatile-token scrubbing).
