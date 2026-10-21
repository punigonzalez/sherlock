package com.sherlock.logdetective.investigation.adapter.out.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sherlock.logdetective.correlation.domain.EvidenceId;
import com.sherlock.logdetective.investigation.domain.InvestigationReport;
import com.sherlock.logdetective.investigation.domain.InvestigationReport.Confidence;
import com.sherlock.logdetective.investigation.domain.InvestigationReport.InvestigationStatus;
import com.sherlock.logdetective.investigation.domain.InvestigationReport.ToolCallRecord;
import com.sherlock.logdetective.investigation.port.out.InvestigationReportRepository;

@Repository
@Transactional
public class JpaInvestigationReportRepository implements InvestigationReportRepository {

	private final SpringDataInvestigationReportJpaRepository reports;

	public JpaInvestigationReportRepository(SpringDataInvestigationReportJpaRepository reports) {
		this.reports = reports;
	}

	@Override
	public InvestigationReport save(InvestigationReport report) {
		InvestigationReportEntity entity = reports.findById(report.id()).orElseGet(InvestigationReportEntity::new);
		entity.setId(report.id());
		entity.setIncidentId(report.incidentId());
		entity.setStatus(report.status().name());
		entity.setProvider(report.provider());
		entity.setSummary(report.summary());
		entity.setHypothesis(report.hypothesis());
		entity.setConfidence(report.confidence().name());
		entity.setEvidenceCitationIds(report.evidenceCitations().stream()
				.map(id -> id.value().toString())
				.toList());
		List<Map<String, String>> toolCalls = new ArrayList<>();
		for (ToolCallRecord call : report.toolCalls()) {
			Map<String, String> row = new LinkedHashMap<>();
			row.put("tool", call.tool());
			row.put("outcome", call.outcome());
			row.put("detail", call.detail());
			toolCalls.add(row);
		}
		entity.setToolCalls(toolCalls);
		entity.setMetadata(new HashMap<>(report.metadata()));
		entity.setCreatedAt(report.createdAt());
		return toDomain(reports.saveAndFlush(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<InvestigationReport> findById(UUID id) {
		return reports.findById(id).map(JpaInvestigationReportRepository::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<InvestigationReport> findLatestByIncidentId(UUID incidentId) {
		return reports.findFirstByIncidentIdOrderByCreatedAtDesc(incidentId)
				.map(JpaInvestigationReportRepository::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<InvestigationReport> findByIncidentId(UUID incidentId) {
		return reports.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream()
				.map(JpaInvestigationReportRepository::toDomain)
				.toList();
	}

	private static InvestigationReport toDomain(InvestigationReportEntity entity) {
		List<EvidenceId> citations = entity.getEvidenceCitationIds() == null
				? List.of()
				: entity.getEvidenceCitationIds().stream()
						.map(UUID::fromString)
						.map(EvidenceId::new)
						.toList();
		List<ToolCallRecord> toolCalls = new ArrayList<>();
		if (entity.getToolCalls() != null) {
			for (Map<String, String> row : entity.getToolCalls()) {
				toolCalls.add(new ToolCallRecord(
						row.getOrDefault("tool", "unknown"),
						row.getOrDefault("outcome", "UNKNOWN"),
						row.getOrDefault("detail", "")
				));
			}
		}
		return new InvestigationReport(
				entity.getId(),
				entity.getIncidentId(),
				InvestigationStatus.valueOf(entity.getStatus()),
				entity.getProvider(),
				entity.getSummary(),
				entity.getHypothesis(),
				Confidence.valueOf(entity.getConfidence()),
				citations,
				toolCalls,
				entity.getMetadata() == null ? Map.of() : entity.getMetadata(),
				entity.getCreatedAt()
		);
	}
}
