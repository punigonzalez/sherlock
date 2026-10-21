package com.sherlock.logdetective.detection.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.sherlock.logdetective.ingestion.domain.EventLevel;

/**
 * An observed error used to create or update an incident. Unknown fields stay empty —
 * do not invent telemetry.
 */
public record ErrorObservation(
		Instant observedAt,
		EventLevel level,
		String message,
		Optional<String> logger,
		Optional<String> service,
		Optional<String> host,
		Optional<String> evidenceRef,
		Severity severity,
		Map<String, String> metadata
) {

	public ErrorObservation {
		Objects.requireNonNull(observedAt, "observedAt");
		Objects.requireNonNull(level, "level");
		Objects.requireNonNull(message, "message");
		if (message.isBlank()) {
			throw new IllegalArgumentException("message must not be blank");
		}
		logger = emptyIfBlank(logger);
		service = emptyIfBlank(service);
		host = emptyIfBlank(host);
		evidenceRef = emptyIfBlank(evidenceRef);
		severity = severity == null ? Severity.MEDIUM : severity;
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}

	private static Optional<String> emptyIfBlank(Optional<String> value) {
		if (value == null || value.isEmpty()) {
			return Optional.empty();
		}
		String trimmed = value.get().trim();
		return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
	}
}
