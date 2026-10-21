package com.sherlock.logdetective.api.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sherlock.logdetective.api.dto.InvestigationDtos.InvestigationResponse;
import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase;

@RestController
@RequestMapping("/api/v1/incidents/{incidentId}/investigations")
public class InvestigationController {

	private final RequestInvestigationUseCase requestInvestigationUseCase;

	public InvestigationController(RequestInvestigationUseCase requestInvestigationUseCase) {
		this.requestInvestigationUseCase = requestInvestigationUseCase;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public InvestigationResponse request(@PathVariable UUID incidentId) {
		return requestInvestigationUseCase.request(incidentId)
				.map(InvestigationResponse::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "incident not found"));
	}

	@GetMapping("/latest")
	public InvestigationResponse latest(@PathVariable UUID incidentId) {
		return requestInvestigationUseCase.latest(incidentId)
				.map(InvestigationResponse::from)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"no investigation report available for incident"
				));
	}
}
