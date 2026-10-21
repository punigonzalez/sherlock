package com.sherlock.logdetective.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.sherlock.logdetective.correlation.domain.Evidence;

public final class EvidenceDtos {

	private EvidenceDtos() {
	}

	public record EvidenceResponse(
			UUID id,
			String kind,
			String strength,
			String reason,
			String summary,
			Set<UUID> eventIds,
			Map<String, String> attributes,
			Instant createdAt
	) {
		public static EvidenceResponse from(Evidence evidence) {
			return new EvidenceResponse(
					evidence.id().value(),
					evidence.kind().name(),
					evidence.strength().name(),
					evidence.reason(),
					evidence.summary(),
					evidence.eventIds(),
					evidence.attributes(),
					evidence.createdAt()
			);
		}
	}
}
