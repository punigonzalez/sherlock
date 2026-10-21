package com.sherlock.logdetective.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase.InvestigationView;
import com.sherlock.logdetective.investigation.domain.InvestigationReport.InvestigationStatus;

public final class InvestigationDtos {

	private InvestigationDtos() {
	}

	public record InvestigationResponse(
			UUID reportId,
			UUID incidentId,
			String status,
			String provider,
			boolean aiUnavailable,
			String summary,
			String hypothesis,
			String confidence,
			List<String> evidenceCitationIds,
			List<String> toolCalls,
			Instant createdAt,
			String note
	) {
		public static InvestigationResponse from(InvestigationView view) {
			String note = null;
			if (view.status() == InvestigationStatus.NOOP_UNAVAILABLE || view.aiUnavailable()) {
				note = "AI diagnosis unavailable or NoOp; deterministic detection remains valid.";
			}
			return new InvestigationResponse(
					view.reportId(),
					view.incidentId(),
					view.status().name(),
					view.provider(),
					view.aiUnavailable(),
					view.summary(),
					view.hypothesis(),
					view.confidence(),
					view.evidenceCitationIds(),
					view.toolCalls(),
					view.createdAt(),
					note
			);
		}
	}
}
