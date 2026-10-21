package com.sherlock.logdetective.investigation.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.sherlock.logdetective.correlation.domain.EvidenceId;
import com.sherlock.logdetective.investigation.domain.InvestigationReport;
import com.sherlock.logdetective.investigation.port.out.AiInvestigator;

public final class InvestigateIncidentUseCase {

	private final AiInvestigator aiInvestigator;

	public InvestigateIncidentUseCase(AiInvestigator aiInvestigator) {
		this.aiInvestigator = Objects.requireNonNull(aiInvestigator, "aiInvestigator");
	}

	public InvestigationReport investigate(UUID incidentId, List<EvidenceId> evidenceIds) {
		return aiInvestigator.investigate(incidentId, evidenceIds);
	}
}
