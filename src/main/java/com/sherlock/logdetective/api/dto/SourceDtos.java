package com.sherlock.logdetective.api.dto;

import com.sherlock.logdetective.ingestion.application.IngestBatchResult;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase.SourceView;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase.ValidationResult;

import jakarta.validation.constraints.NotBlank;

public final class SourceDtos {

	private SourceDtos() {
	}

	public record ValidateSourceRequest(
			@NotBlank String id,
			@NotBlank String path,
			String encoding
	) {
	}

	public record RegisterSourceRequest(
			@NotBlank String id,
			@NotBlank String path,
			String encoding
	) {
	}

	public record SourceResponse(
			String id,
			String type,
			String path,
			boolean running,
			String healthStatus,
			String healthDetail
	) {
		public static SourceResponse from(SourceView view) {
			return new SourceResponse(
					view.id(),
					view.type(),
					view.path(),
					view.running(),
					view.healthStatus(),
					view.healthDetail()
			);
		}
	}

	public record ValidationResponse(
			boolean valid,
			String sourceId,
			String resolvedPath,
			String status,
			String detail,
			String errorMessage
	) {
		public static ValidationResponse from(ValidationResult result) {
			return new ValidationResponse(
					result.valid(),
					result.sourceId(),
					result.resolvedPath(),
					result.status(),
					result.detail(),
					result.errorMessage()
			);
		}
	}

	public record IngestResponse(
			int fetched,
			int accepted,
			int duplicates,
			int parseFailures,
			int errorsRoutedToIncidents
	) {
		public static IngestResponse from(IngestBatchResult result) {
			return new IngestResponse(
					result.fetched(),
					result.accepted(),
					result.duplicates(),
					result.parseFailures(),
					result.errorsRoutedToIncidents()
			);
		}
	}
}
