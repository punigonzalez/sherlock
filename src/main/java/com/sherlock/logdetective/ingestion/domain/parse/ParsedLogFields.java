package com.sherlock.logdetective.ingestion.domain.parse;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.sherlock.logdetective.ingestion.domain.EventLevel;

public record ParsedLogFields(
		Instant timestamp,
		EventLevel level,
		String message,
		Optional<String> logger,
		Optional<String> service,
		Optional<String> host,
		Optional<String> traceId,
		Optional<String> spanId,
		Map<String, String> attributes
) {

	public ParsedLogFields {
		Objects.requireNonNull(timestamp, "timestamp");
		Objects.requireNonNull(level, "level");
		Objects.requireNonNull(message, "message");
		logger = emptyIfBlank(logger);
		service = emptyIfBlank(service);
		host = emptyIfBlank(host);
		traceId = emptyIfBlank(traceId);
		spanId = emptyIfBlank(spanId);
		attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
	}

	private static Optional<String> emptyIfBlank(Optional<String> value) {
		if (value == null || value.isEmpty()) {
			return Optional.empty();
		}
		String trimmed = value.get().trim();
		return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
	}
}
