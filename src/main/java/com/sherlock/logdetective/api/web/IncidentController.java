package com.sherlock.logdetective.api.web;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sherlock.logdetective.api.dto.EvidenceDtos.EvidenceResponse;
import com.sherlock.logdetective.api.dto.IncidentDtos.IncidentDetailResponse;
import com.sherlock.logdetective.api.dto.IncidentDtos.IncidentSummaryResponse;
import com.sherlock.logdetective.api.dto.PageResponse;
import com.sherlock.logdetective.correlation.application.QueryEvidenceUseCase;
import com.sherlock.logdetective.detection.application.QueryIncidentsUseCase;
import com.sherlock.logdetective.detection.domain.IncidentStatus;
import com.sherlock.logdetective.shared.domain.PageResult;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

	private final QueryIncidentsUseCase queryIncidentsUseCase;
	private final QueryEvidenceUseCase queryEvidenceUseCase;

	public IncidentController(
			QueryIncidentsUseCase queryIncidentsUseCase,
			QueryEvidenceUseCase queryEvidenceUseCase
	) {
		this.queryIncidentsUseCase = queryIncidentsUseCase;
		this.queryEvidenceUseCase = queryEvidenceUseCase;
	}

	@GetMapping
	public PageResponse<IncidentSummaryResponse> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String status
	) {
		Optional<IncidentStatus> statusFilter = Optional.empty();
		if (status != null && !status.isBlank()) {
			try {
				statusFilter = Optional.of(IncidentStatus.valueOf(status.trim().toUpperCase()));
			}
			catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException("unknown status: " + status);
			}
		}
		PageResult<com.sherlock.logdetective.detection.domain.Incident> result =
				queryIncidentsUseCase.list(page, size, statusFilter);
		return new PageResponse<>(
				result.items().stream().map(IncidentSummaryResponse::from).toList(),
				result.page(),
				result.size(),
				result.totalElements(),
				result.totalPages()
		);
	}

	@GetMapping("/{id}")
	public IncidentDetailResponse get(@PathVariable UUID id) {
		return queryIncidentsUseCase.get(id)
				.map(IncidentDetailResponse::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "incident not found"));
	}

	@GetMapping("/{id}/evidence")
	public List<EvidenceResponse> evidence(@PathVariable UUID id) {
		if (queryIncidentsUseCase.get(id).isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "incident not found");
		}
		return queryEvidenceUseCase.forIncident(id).stream().map(EvidenceResponse::from).toList();
	}
}
