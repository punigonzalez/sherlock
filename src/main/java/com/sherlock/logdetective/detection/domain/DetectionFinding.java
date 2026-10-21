package com.sherlock.logdetective.detection.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.sherlock.logdetective.correlation.domain.CorrelationStrength;

/**
 * Deterministic detection/correlation outcome with a reproducible reason.
 */
public record DetectionFinding(
		DetectionKind kind,
		Severity severity,
		String reason,
		String title,
		Optional<String> service,
		Optional<String> traceId,
		CorrelationStrength strength,
		Set<UUID> contributingEventIds,
		Map<String, String> metrics,
		Instant detectedAt
) {

	public DetectionFinding {
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(severity, "severity");
		Objects.requireNonNull(reason, "reason");
		if (reason.isBlank()) {
			throw new IllegalArgumentException("reason must not be blank");
		}
		Objects.requireNonNull(title, "title");
		service = emptyIfBlank(service);
		traceId = emptyIfBlank(traceId);
		strength = strength == null ? CorrelationStrength.NONE : strength;
		contributingEventIds = contributingEventIds == null
				? Set.of()
				: Set.copyOf(new LinkedHashSet<>(contributingEventIds));
		metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
		Objects.requireNonNull(detectedAt, "detectedAt");
	}

	private static Optional<String> emptyIfBlank(Optional<String> value) {
		if (value == null || value.isEmpty()) {
			return Optional.empty();
		}
		String trimmed = value.get().trim();
		return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
	}
}
