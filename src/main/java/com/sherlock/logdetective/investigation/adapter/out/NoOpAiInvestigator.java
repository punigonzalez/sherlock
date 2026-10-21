package com.sherlock.logdetective.investigation.adapter.out;

import java.util.List;
import java.util.UUID;

import com.sherlock.logdetective.correlation.domain.EvidenceId;
import com.sherlock.logdetective.investigation.domain.InvestigationReport;
import com.sherlock.logdetective.investigation.port.out.AiInvestigator;

/**
 * Default AI adapter: no external provider required.
 */
public final class NoOpAiInvestigator implements AiInvestigator {

	@Override
	public InvestigationReport investigate(UUID incidentId, List<EvidenceId> evidenceIds) {
		List<EvidenceId> citations = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
		return new InvestigationReport(
				incidentId,
				"AI investigation disabled (NoOp). Deterministic detection remains available.",
				citations
		);
	}
}
