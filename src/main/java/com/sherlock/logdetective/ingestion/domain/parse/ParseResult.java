package com.sherlock.logdetective.ingestion.domain.parse;

import java.util.Objects;
import java.util.Optional;

/**
 * Parse outcome. Failures are explicit — callers must not discard the raw payload.
 */
public record ParseResult(
		boolean success,
		Optional<ParsedLogFields> fields,
		Optional<String> failureReason
) {

	public ParseResult {
		Objects.requireNonNull(fields, "fields");
		Objects.requireNonNull(failureReason, "failureReason");
		if (success && fields.isEmpty()) {
			throw new IllegalArgumentException("successful parse requires fields");
		}
		if (!success && failureReason.isEmpty()) {
			throw new IllegalArgumentException("failed parse requires failureReason");
		}
	}

	public static ParseResult ok(ParsedLogFields fields) {
		return new ParseResult(true, Optional.of(fields), Optional.empty());
	}

	public static ParseResult failure(String reason) {
		return new ParseResult(false, Optional.empty(), Optional.of(reason));
	}
}
