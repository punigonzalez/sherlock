package com.sherlock.logdetective.investigation.adapter.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sherlock.logdetective.correlation.domain.EvidenceId;
import com.sherlock.logdetective.investigation.domain.InvestigationReport;

class NoOpAiInvestigatorTest {

	private final NoOpAiInvestigator investigator = new NoOpAiInvestigator();

	@Test
	void returnsReportGroundedInProvidedEvidenceOnly() {
		UUID incidentId = UUID.randomUUID();
		EvidenceId evidenceId = EvidenceId.generate();

		InvestigationReport report = investigator.investigate(incidentId, List.of(evidenceId));

		assertThat(report.incidentId()).isEqualTo(incidentId);
		assertThat(report.evidenceCitations()).containsExactly(evidenceId);
		assertThat(report.summary()).contains("NoOp");
	}
}
