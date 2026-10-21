package com.sherlock.logdetective.detection.application;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.IncidentStatus;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;
import com.sherlock.logdetective.shared.domain.PageResult;

public final class QueryIncidentsUseCase {

	private final IncidentRepository incidentRepository;

	public QueryIncidentsUseCase(IncidentRepository incidentRepository) {
		this.incidentRepository = Objects.requireNonNull(incidentRepository);
	}

	public PageResult<Incident> list(int page, int size, Optional<IncidentStatus> status) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);
		return incidentRepository.findPage(safePage, safeSize, status == null ? Optional.empty() : status);
	}

	public Optional<Incident> get(UUID id) {
		return incidentRepository.findById(Objects.requireNonNull(id));
	}
}
