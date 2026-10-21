package com.sherlock.logdetective.investigation.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sherlock.logdetective.investigation.domain.InvestigationReport;

public interface InvestigationReportRepository {

	InvestigationReport save(InvestigationReport report);

	Optional<InvestigationReport> findById(UUID id);

	Optional<InvestigationReport> findLatestByIncidentId(UUID incidentId);

	List<InvestigationReport> findByIncidentId(UUID incidentId);
}
