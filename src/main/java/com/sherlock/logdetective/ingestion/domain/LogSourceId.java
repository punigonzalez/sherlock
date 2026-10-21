package com.sherlock.logdetective.ingestion.domain;

import java.util.Objects;

public record LogSourceId(String value) {

	public LogSourceId {
		Objects.requireNonNull(value, "value");
		if (value.isBlank()) {
			throw new IllegalArgumentException("LogSourceId must not be blank");
		}
	}
}
