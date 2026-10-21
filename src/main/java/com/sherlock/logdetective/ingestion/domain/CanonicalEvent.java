package com.sherlock.logdetective.ingestion.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Normalized log event. Optional telemetry fields are {@link Optional#empty()} when unknown —
 * callers must not invent placeholders for missing data.
 */
public record CanonicalEvent(
		UUID id,
		LogSourceId sourceId,
		String sourcePayloadId,
		Instant timestamp,
		Instant ingestedAt,
		EventLevel level,
		String message,
		Optional<String> logger,
		Optional<String> service,
		Optional<String> host,
		Optional<String> traceId,
		Optional<String> spanId,
		Map<String, String> attributes
) {

	public CanonicalEvent {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(sourceId, "sourceId");
		Objects.requireNonNull(sourcePayloadId, "sourcePayloadId");
		Objects.requireNonNull(timestamp, "timestamp");
		Objects.requireNonNull(ingestedAt, "ingestedAt");
		Objects.requireNonNull(level, "level");
		Objects.requireNonNull(message, "message");
		logger = normalizeOptional(logger);
		service = normalizeOptional(service);
		host = normalizeOptional(host);
		traceId = normalizeOptional(traceId);
		spanId = normalizeOptional(spanId);
		attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
	}

	public static CanonicalEvent of(
			UUID id,
			LogSourceId sourceId,
			String sourcePayloadId,
			Instant timestamp,
			Instant ingestedAt,
			EventLevel level,
			String message,
			Map<String, String> attributes
	) {
		return new CanonicalEvent(
				id,
				sourceId,
				sourcePayloadId,
				timestamp,
				ingestedAt,
				level,
				message,
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				attributes
		);
	}

	private static Optional<String> normalizeOptional(Optional<String> value) {
		if (value == null || value.isEmpty()) {
			return Optional.empty();
		}
		String trimmed = value.get().trim();
		return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
	}
}
