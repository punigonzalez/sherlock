package com.sherlock.logdetective.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase.InvestigationView;

public final class InvestigationDtos {

	private InvestigationDtos() {
	}

	public record InvestigationResponse(
			UUID incidentId,
			String status,
			boolean aiUnavailable,
			String summary,
			List<String> evidenceCitationIds,
			Instant createdAt,
			String note
	) {
		public static InvestigationResponse from(InvestigationView view) {
			String note = view.aiUnavailable()
					? "AI investigator is NoOp; this is not a real diagnosis."
					: null;
			return new InvestigationResponse(
					view.incidentId(),
					view.status().name(),
					view.aiUnavailable(),
					view.summary(),
					view.evidenceCitationIds(),
					view.createdAt(),
					note
			);
		}
	}
}
