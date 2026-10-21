package com.sherlock.logdetective.ingestion.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Normalized log event. Persistence mapping arrives in a later phase.
 */
public record CanonicalEvent(
		UUID id,
		LogSourceId sourceId,
		String sourcePayloadId,
		Instant timestamp,
		Instant ingestedAt,
		EventLevel level,
		String message,
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
		attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
	}
}
