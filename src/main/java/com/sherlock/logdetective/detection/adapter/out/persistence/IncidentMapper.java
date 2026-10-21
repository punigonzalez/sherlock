package com.sherlock.logdetective.detection.adapter.out.persistence;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sherlock.logdetective.detection.domain.ErrorFingerprint;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.IncidentStatus;
import com.sherlock.logdetective.detection.domain.Severity;

final class IncidentMapper {

	private IncidentMapper() {
	}

	static Incident toDomain(IncidentEntity entity) {
		ErrorFingerprint fingerprint = new ErrorFingerprint(
				entity.getFingerprint().getFingerprint(),
				entity.getFingerprint().getNormalizedSignature()
		);

		List<Incident.StatusTransition> history = entity.getStatusHistory().stream()
				.sorted((a, b) -> a.getChangedAt().compareTo(b.getChangedAt()))
				.map(h -> new Incident.StatusTransition(
						h.getFromStatus() == null ? null : IncidentStatus.valueOf(h.getFromStatus()),
						IncidentStatus.valueOf(h.getToStatus()),
						h.getChangedAt(),
						h.getReason()
				))
				.toList();

		Set<String> evidence = entity.getEvidenceLinks().stream()
				.map(IncidentEvidenceLinkEntity::getEvidenceRef)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		return Incident.rehydrate(
				entity.getId(),
				fingerprint,
				IncidentStatus.valueOf(entity.getStatus()),
				Severity.valueOf(entity.getSeverity()),
				entity.getTitle(),
				entity.getFirstSeenAt(),
				entity.getLastSeenAt(),
				entity.getOccurrenceCount(),
				entity.getMetadata() == null ? Map.of() : entity.getMetadata(),
				evidence,
				history
		);
	}

	static void applyDomain(Incident domain, IncidentEntity entity, ErrorFingerprintEntity fingerprintEntity) {
		Instant now = Instant.now();
		entity.setId(domain.id());
		entity.setFingerprint(fingerprintEntity);
		entity.setStatus(domain.status().name());
		entity.setSeverity(domain.severity().name());
		entity.setTitle(domain.title());
		entity.setFirstSeenAt(domain.firstSeenAt());
		entity.setLastSeenAt(domain.lastSeenAt());
		entity.setOccurrenceCount(domain.occurrenceCount());
		entity.setMetadata(Map.copyOf(domain.metadata()));
		if (entity.getCreatedAt() == null) {
			entity.setCreatedAt(now);
		}
		entity.setUpdatedAt(now);

		entity.getStatusHistory().clear();
		for (Incident.StatusTransition transition : domain.history()) {
			entity.getStatusHistory().add(new IncidentStatusHistoryEntity(
					entity,
					transition.from() == null ? null : transition.from().name(),
					transition.to().name(),
					transition.at(),
					transition.reason()
			));
		}

		Set<String> existing = entity.getEvidenceLinks().stream()
				.map(IncidentEvidenceLinkEntity::getEvidenceRef)
				.collect(Collectors.toSet());
		for (String ref : domain.evidenceRefs()) {
			if (!existing.contains(ref)) {
				entity.getEvidenceLinks().add(new IncidentEvidenceLinkEntity(
						UUID.randomUUID(),
						entity,
						ref,
						domain.lastSeenAt()
				));
			}
		}
	}
}
