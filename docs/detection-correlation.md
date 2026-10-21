# Detection & correlation (Phase 4)

Deterministic rules only — no ML. Config under `logdetective.detection.*`.

## Severity

`SeverityClassifier`: `ERROR→HIGH`, `WARN→MEDIUM`, `INFO/DEBUG/TRACE→LOW`, `UNKNOWN→MEDIUM`.  
Ingest still routes `ERROR` events into fingerprint incidents (`ERROR_SEVERITY`).

## Threshold rate

Per service (or `_unknown`), count `ERROR` events in `threshold.window-seconds`.  
Fires when count ≥ `threshold.min-error-count`.

## Spike

Compare current window count to the average of the previous `spike.baseline-windows` windows.  
Fires when `current >= max(min-error-count, baselineAvg * multiplier)`.

## Trace correlation

Events sharing `traceId` within `correlation.trace-lookback-seconds`:

| Strength | Condition |
|----------|-----------|
| CONFIRMED | ≥2 distinct services |
| STRONG | same service, ≥3 events |
| WEAK | otherwise (≥2 events) |

Reasons explicitly state this is **not** a causal claim.

## Temporal correlation

`ERROR`s from different services within `correlation.temporal-window-seconds`:

| Strength | Condition |
|----------|-----------|
| STRONG | shared message token (≥5 chars) |
| WEAK | proximity only |

Pairs already sharing a trace ID are skipped (handled by trace correlation). No causal language.

## Evidence

Each finding creates an `evidence` row with reproducible `reason`, optional `strength`, linked event IDs, and an `incident_evidence_links` row (`evidence_ref` = evidence UUID).
