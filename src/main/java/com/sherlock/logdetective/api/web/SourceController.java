package com.sherlock.logdetective.api.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sherlock.logdetective.api.dto.SourceDtos.IngestResponse;
import com.sherlock.logdetective.api.dto.SourceDtos.RegisterSourceRequest;
import com.sherlock.logdetective.api.dto.SourceDtos.SourceResponse;
import com.sherlock.logdetective.api.dto.SourceDtos.ValidateSourceRequest;
import com.sherlock.logdetective.api.dto.SourceDtos.ValidationResponse;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sources")
public class SourceController {

	private final ManageFileSourcesUseCase manageFileSourcesUseCase;

	public SourceController(ManageFileSourcesUseCase manageFileSourcesUseCase) {
		this.manageFileSourcesUseCase = manageFileSourcesUseCase;
	}

	@GetMapping
	public List<SourceResponse> list() {
		return manageFileSourcesUseCase.list().stream().map(SourceResponse::from).toList();
	}

	@PostMapping("/validate")
	public ValidationResponse validate(@Valid @RequestBody ValidateSourceRequest request) {
		return ValidationResponse.from(
				manageFileSourcesUseCase.validate(request.id(), request.path(), request.encoding())
		);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SourceResponse register(@Valid @RequestBody RegisterSourceRequest request) {
		return SourceResponse.from(
				manageFileSourcesUseCase.registerAndStart(request.id(), request.path(), request.encoding())
		);
	}

	@GetMapping("/{id}/health")
	public SourceResponse health(@PathVariable String id) {
		return manageFileSourcesUseCase.health(id)
				.map(SourceResponse::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "source not found"));
	}

	@PostMapping("/{id}/ingest")
	public IngestResponse ingest(@PathVariable String id) {
		return IngestResponse.from(manageFileSourcesUseCase.ingestOnce(id));
	}
}
