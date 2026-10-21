package com.sherlock.logdetective.correlation.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Reproducible evidence supporting an incident or correlation finding.
 */
public record Evidence(
		EvidenceId id,
		EvidenceKind kind,
		CorrelationStrength strength,
		String reason,
		String summary,
		Set<UUID> eventIds,
		Map<String, String> attributes,
		Instant createdAt
) {

	public Evidence {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(strength, "strength");
		Objects.requireNonNull(reason, "reason");
		if (reason.isBlank()) {
			throw new IllegalArgumentException("reason must not be blank");
		}
		Objects.requireNonNull(summary, "summary");
		eventIds = eventIds == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(eventIds));
		attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
		Objects.requireNonNull(createdAt, "createdAt");
	}

	public static Evidence of(
			EvidenceKind kind,
			CorrelationStrength strength,
			String reason,
			String summary,
			Set<UUID> eventIds,
			Map<String, String> attributes
	) {
		return new Evidence(
				EvidenceId.generate(),
				kind,
				strength,
				reason,
				summary,
				eventIds,
				attributes,
				Instant.now()
		);
	}
}
