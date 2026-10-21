package com.sherlock.logdetective.investigation.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.sherlock.logdetective.correlation.domain.EvidenceId;

/**
 * Evidence-grounded investigation output. Citations must reference real evidence ids.
 */
public record InvestigationReport(
		UUID incidentId,
		String summary,
		List<EvidenceId> evidenceCitations
) {

	public InvestigationReport {
		Objects.requireNonNull(incidentId, "incidentId");
		Objects.requireNonNull(summary, "summary");
		evidenceCitations = evidenceCitations == null ? List.of() : List.copyOf(evidenceCitations);
	}
}
