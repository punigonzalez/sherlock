package com.sherlock.logdetective.investigation.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataInvestigationReportJpaRepository extends JpaRepository<InvestigationReportEntity, UUID> {

	Optional<InvestigationReportEntity> findFirstByIncidentIdOrderByCreatedAtDesc(UUID incidentId);

	List<InvestigationReportEntity> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);
}
