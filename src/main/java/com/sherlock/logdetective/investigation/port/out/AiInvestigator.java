package com.sherlock.logdetective.investigation.port.out;

import java.util.List;
import java.util.UUID;

import com.sherlock.logdetective.correlation.domain.EvidenceId;
import com.sherlock.logdetective.investigation.domain.InvestigationReport;

/**
 * Optional outbound port for AI-assisted investigation.
 * Deterministic paths must work without a real provider.
 */
public interface AiInvestigator {

	InvestigationReport investigate(UUID incidentId, List<EvidenceId> evidenceIds);
}
