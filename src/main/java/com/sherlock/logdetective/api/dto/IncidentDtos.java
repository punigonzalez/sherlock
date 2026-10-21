package com.sherlock.logdetective.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sherlock.logdetective.detection.domain.Incident;

public final class IncidentDtos {

	private IncidentDtos() {
	}

	public record IncidentSummaryResponse(
			UUID id,
			String status,
			String severity,
			String title,
			String fingerprint,
			Instant firstSeenAt,
			Instant lastSeenAt,
			long occurrenceCount
	) {
		public static IncidentSummaryResponse from(Incident incident) {
			return new IncidentSummaryResponse(
					incident.id(),
					incident.status().name(),
					incident.severity().name(),
					incident.title(),
					incident.fingerprint().value(),
					incident.firstSeenAt(),
					incident.lastSeenAt(),
					incident.occurrenceCount()
			);
		}
	}

	public record IncidentDetailResponse(
			UUID id,
			String status,
			String severity,
			String title,
			String fingerprint,
			String normalizedSignature,
			Instant firstSeenAt,
			Instant lastSeenAt,
			long occurrenceCount,
			Map<String, String> metadata,
			Set<String> evidenceRefs,
			List<StatusTransitionResponse> history
	) {
		public static IncidentDetailResponse from(Incident incident) {
			return new IncidentDetailResponse(
					incident.id(),
					incident.status().name(),
					incident.severity().name(),
					incident.title(),
					incident.fingerprint().value(),
					incident.fingerprint().normalizedSignature(),
					incident.firstSeenAt(),
					incident.lastSeenAt(),
					incident.occurrenceCount(),
					incident.metadata(),
					incident.evidenceRefs(),
					incident.history().stream()
							.map(h -> new StatusTransitionResponse(
									h.from() == null ? null : h.from().name(),
									h.to().name(),
									h.at(),
									h.reason()
							))
							.toList()
			);
		}
	}

	public record StatusTransitionResponse(
			String from,
			String to,
			Instant at,
			String reason
	) {
	}
}
