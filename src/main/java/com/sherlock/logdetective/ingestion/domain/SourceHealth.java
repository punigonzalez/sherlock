package com.sherlock.logdetective.ingestion.domain;

import java.time.Instant;
import java.util.Objects;

public record SourceHealth(
		HealthStatus status,
		String detail,
		Instant checkedAt
) {

	public SourceHealth {
		Objects.requireNonNull(status, "status");
		detail = detail == null ? "" : detail;
		Objects.requireNonNull(checkedAt, "checkedAt");
	}

	public static SourceHealth of(HealthStatus status, String detail) {
		return new SourceHealth(status, detail, Instant.now());
	}

	public enum HealthStatus {
		HEALTHY,
		DEGRADED,
		UNAVAILABLE,
		PERMISSION_DENIED,
		NOT_STARTED,
		STOPPED
	}
}
