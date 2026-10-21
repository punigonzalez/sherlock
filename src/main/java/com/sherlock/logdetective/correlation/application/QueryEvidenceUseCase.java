package com.sherlock.logdetective.correlation.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.sherlock.logdetective.correlation.domain.Evidence;
import com.sherlock.logdetective.correlation.domain.EvidenceId;
import com.sherlock.logdetective.correlation.port.out.EvidenceRepository;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;

public final class QueryEvidenceUseCase {

	private final EvidenceRepository evidenceRepository;
	private final IncidentRepository incidentRepository;

	public QueryEvidenceUseCase(EvidenceRepository evidenceRepository, IncidentRepository incidentRepository) {
		this.evidenceRepository = Objects.requireNonNull(evidenceRepository);
		this.incidentRepository = Objects.requireNonNull(incidentRepository);
	}

	public List<Evidence> forIncident(UUID incidentId) {
		Objects.requireNonNull(incidentId);
		if (incidentRepository.findById(incidentId).isEmpty()) {
			return List.of();
		}
		return evidenceRepository.findByIncidentId(incidentId);
	}

	public Optional<Evidence> get(EvidenceId id) {
		return evidenceRepository.findById(Objects.requireNonNull(id));
	}
}
