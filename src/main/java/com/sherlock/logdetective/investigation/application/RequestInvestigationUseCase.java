package com.sherlock.logdetective.investigation.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.sherlock.logdetective.correlation.domain.Evidence;
import com.sherlock.logdetective.correlation.domain.EvidenceId;
import com.sherlock.logdetective.correlation.port.out.EvidenceRepository;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.IncidentStatus;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;
import com.sherlock.logdetective.investigation.domain.InvestigationReport;
import com.sherlock.logdetective.investigation.domain.InvestigationReport.InvestigationStatus;
import com.sherlock.logdetective.investigation.port.out.InvestigationReportRepository;

/**
 * Investigation entrypoint: runs AI (or NoOp/Fake), persists report, advances incident lifecycle.
 */
public final class RequestInvestigationUseCase {

	private final IncidentRepository incidentRepository;
	private final EvidenceRepository evidenceRepository;
	private final InvestigateIncidentUseCase investigateIncidentUseCase;
	private final InvestigationReportRepository reportRepository;

	public RequestInvestigationUseCase(
			IncidentRepository incidentRepository,
			EvidenceRepository evidenceRepository,
			InvestigateIncidentUseCase investigateIncidentUseCase,
			InvestigationReportRepository reportRepository
	) {
		this.incidentRepository = Objects.requireNonNull(incidentRepository);
		this.evidenceRepository = Objects.requireNonNull(evidenceRepository);
		this.investigateIncidentUseCase = Objects.requireNonNull(investigateIncidentUseCase);
		this.reportRepository = Objects.requireNonNull(reportRepository);
	}

	public Optional<InvestigationView> request(UUID incidentId) {
		Optional<Incident> incident = incidentRepository.findById(incidentId);
		if (incident.isEmpty()) {
			return Optional.empty();
		}
		List<EvidenceId> evidenceIds = evidenceRepository.findByIncidentId(incidentId).stream()
				.map(Evidence::id)
				.toList();
		InvestigationReport report = investigateIncidentUseCase.investigate(incidentId, evidenceIds);
		InvestigationReport saved = reportRepository.save(report);

		Incident current = incident.get();
		if (current.status() == IncidentStatus.OPEN || current.status() == IncidentStatus.REOPENED) {
			current.transitionTo(IncidentStatus.INVESTIGATING, saved.createdAt(), "investigation requested");
			incidentRepository.save(current);
		}

		return Optional.of(toView(saved));
	}

	public Optional<InvestigationView> latest(UUID incidentId) {
		return reportRepository.findLatestByIncidentId(incidentId).map(RequestInvestigationUseCase::toView);
	}

	private static InvestigationView toView(InvestigationReport report) {
		boolean unavailable = report.status() == InvestigationStatus.NOOP_UNAVAILABLE
				|| "true".equalsIgnoreCase(report.metadata().getOrDefault("aiUnavailable", "false"));
		return new InvestigationView(
				report.id(),
				report.incidentId(),
				report.status(),
				report.provider(),
				unavailable,
				report.summary(),
				report.hypothesis(),
				report.confidence().name(),
				report.evidenceCitations().stream().map(id -> id.value().toString()).toList(),
				report.toolCalls().stream()
						.map(c -> c.tool() + ":" + c.outcome())
						.toList(),
				report.createdAt()
		);
	}

	public record InvestigationView(
			UUID reportId,
			UUID incidentId,
			InvestigationStatus status,
			String provider,
			boolean aiUnavailable,
			String summary,
			String hypothesis,
			String confidence,
			List<String> evidenceCitationIds,
			List<String> toolCalls,
			java.time.Instant createdAt
	) {
	}
}
