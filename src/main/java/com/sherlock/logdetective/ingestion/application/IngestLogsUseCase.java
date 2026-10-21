package com.sherlock.logdetective.ingestion.application;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.sherlock.logdetective.detection.application.RecordErrorObservationUseCase;
import com.sherlock.logdetective.detection.domain.ErrorObservation;
import com.sherlock.logdetective.detection.domain.Severity;
import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.EventLevel;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.domain.parse.LogParser;
import com.sherlock.logdetective.ingestion.domain.parse.ParseResult;
import com.sherlock.logdetective.ingestion.domain.parse.ParsedLogFields;
import com.sherlock.logdetective.ingestion.domain.redaction.RedactionHook;
import com.sherlock.logdetective.ingestion.port.out.EventRepository;
import com.sherlock.logdetective.ingestion.port.out.LogSource;
import com.sherlock.logdetective.ingestion.port.out.LogSource.FetchCursor;
import com.sherlock.logdetective.ingestion.port.out.LogSource.RawLogRecord;
import com.sherlock.logdetective.ingestion.port.out.SourceCheckpointStore;

/**
 * Pipeline: acquire → validate source → parse → normalize → enrich → redact → dedup →
 * deliver errors to detection → checkpoint after safe accept.
 */
public final class IngestLogsUseCase {

	private final EventRepository eventRepository;
	private final SourceCheckpointStore checkpointStore;
	private final LogParser logParser;
	private final RedactionHook redactionHook;
	private final RecordErrorObservationUseCase recordErrorObservationUseCase;

	public IngestLogsUseCase(
			EventRepository eventRepository,
			SourceCheckpointStore checkpointStore,
			LogParser logParser,
			RedactionHook redactionHook,
			RecordErrorObservationUseCase recordErrorObservationUseCase
	) {
		this.eventRepository = Objects.requireNonNull(eventRepository);
		this.checkpointStore = Objects.requireNonNull(checkpointStore);
		this.logParser = Objects.requireNonNull(logParser);
		this.redactionHook = Objects.requireNonNull(redactionHook);
		this.recordErrorObservationUseCase = Objects.requireNonNull(recordErrorObservationUseCase);
	}

	public IngestBatchResult ingest(LogSource source, int limit) {
		Objects.requireNonNull(source, "source");
		if (!source.isRunning()) {
			source.validate();
			source.start();
		}

		FetchCursor cursor = checkpointStore.load(source.id()).orElse(FetchCursor.start());
		List<RawLogRecord> batch = source.fetch(cursor, limit);

		int accepted = 0;
		int duplicates = 0;
		int parseFailures = 0;
		int errorsRouted = 0;

		for (RawLogRecord raw : batch) {
			Instant ingestedAt = Instant.now();
			ParseResult parsed = logParser.parse(raw.rawBody(), raw.observedAt());
			CanonicalEvent event = toCanonical(source.id(), raw, parsed, ingestedAt);

			Optional<CanonicalEvent> existing = eventRepository.findBySourcePayloadId(source.id(), event.sourcePayloadId());
			if (existing.isPresent()) {
				duplicates++;
				continue;
			}

			CanonicalEvent saved = eventRepository.save(event);
			accepted++;
			if (!parsed.success()) {
				parseFailures++;
			}
			if (saved.level() == EventLevel.ERROR) {
				recordErrorObservationUseCase.execute(toObservation(saved));
				errorsRouted++;
			}
		}

		FetchCursor next = source.advance(cursor, batch);
		checkpointStore.save(source.id(), next);
		return new IngestBatchResult(batch.size(), accepted, duplicates, parseFailures, errorsRouted);
	}

	private CanonicalEvent toCanonical(
			LogSourceId sourceId,
			RawLogRecord raw,
			ParseResult parsed,
			Instant ingestedAt
	) {
		if (parsed.success()) {
			ParsedLogFields fields = parsed.fields().orElseThrow();
			String message = redactionHook.redact(fields.message());
			Map<String, String> attrs = new HashMap<>(fields.attributes());
			attrs.putAll(raw.headers());
			return new CanonicalEvent(
					UUID.randomUUID(),
					sourceId,
					raw.sourcePayloadId(),
					fields.timestamp(),
					ingestedAt,
					fields.level(),
					message,
					fields.logger(),
					fields.service(),
					fields.host(),
					fields.traceId(),
					fields.spanId(),
					attrs
			);
		}

		Map<String, String> attrs = new HashMap<>(raw.headers());
		attrs.put("parse_failed", "true");
		attrs.put("parse_failure_reason", parsed.failureReason().orElse("unknown"));
		return new CanonicalEvent(
				UUID.randomUUID(),
				sourceId,
				raw.sourcePayloadId(),
				raw.observedAt(),
				ingestedAt,
				EventLevel.UNKNOWN,
				redactionHook.redact(raw.rawBody()),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				attrs
		);
	}

	private static ErrorObservation toObservation(CanonicalEvent event) {
		Severity severity = switch (event.level()) {
			case ERROR -> Severity.HIGH;
			default -> Severity.MEDIUM;
		};
		return new ErrorObservation(
				event.timestamp(),
				event.level(),
				event.message(),
				event.logger(),
				event.service(),
				event.host(),
				Optional.of(event.id().toString()),
				severity,
				Map.of(
						"sourceId", event.sourceId().value(),
						"sourcePayloadId", event.sourcePayloadId()
				)
		);
	}
}
