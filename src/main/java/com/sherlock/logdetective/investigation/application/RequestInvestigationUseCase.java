package com.sherlock.logdetective.investigation.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.sherlock.logdetective.correlation.domain.Evidence;
import com.sherlock.logdetective.correlation.domain.EvidenceId;
import com.sherlock.logdetective.correlation.port.out.EvidenceRepository;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;
import com.sherlock.logdetective.investigation.domain.InvestigationReport;
import com.sherlock.logdetective.investigation.port.out.AiInvestigator;

/**
 * Honest investigation entrypoint. With NoOp AI, reports are marked unavailable for diagnosis.
 */
public final class RequestInvestigationUseCase {

	private final IncidentRepository incidentRepository;
	private final EvidenceRepository evidenceRepository;
	private final InvestigateIncidentUseCase investigateIncidentUseCase;
	private final AiInvestigator aiInvestigator;
	private final ConcurrentHashMap<UUID, StoredInvestigation> latest = new ConcurrentHashMap<>();

	public RequestInvestigationUseCase(
			IncidentRepository incidentRepository,
			EvidenceRepository evidenceRepository,
			InvestigateIncidentUseCase investigateIncidentUseCase,
			AiInvestigator aiInvestigator
	) {
		this.incidentRepository = Objects.requireNonNull(incidentRepository);
		this.evidenceRepository = Objects.requireNonNull(evidenceRepository);
		this.investigateIncidentUseCase = Objects.requireNonNull(investigateIncidentUseCase);
		this.aiInvestigator = Objects.requireNonNull(aiInvestigator);
	}

	public Optional<InvestigationView> request(UUID incidentId) {
		if (incidentRepository.findById(incidentId).isEmpty()) {
			return Optional.empty();
		}
		List<EvidenceId> evidenceIds = evidenceRepository.findByIncidentId(incidentId).stream()
				.map(Evidence::id)
				.toList();
		InvestigationReport report = investigateIncidentUseCase.investigate(incidentId, evidenceIds);
		boolean noop = aiInvestigator.getClass().getSimpleName().contains("NoOp");
		InvestigationView view = new InvestigationView(
				incidentId,
				noop ? InvestigationStatus.NOOP_UNAVAILABLE : InvestigationStatus.COMPLETED,
				noop,
				report.summary(),
				report.evidenceCitations().stream().map(id -> id.value().toString()).toList(),
				Instant.now()
		);
		latest.put(incidentId, new StoredInvestigation(view));
		return Optional.of(view);
	}

	public Optional<InvestigationView> latest(UUID incidentId) {
		StoredInvestigation stored = latest.get(incidentId);
		return stored == null ? Optional.empty() : Optional.of(stored.view());
	}

	public enum InvestigationStatus {
		NOOP_UNAVAILABLE,
		COMPLETED
	}

	public record InvestigationView(
			UUID incidentId,
			InvestigationStatus status,
			boolean aiUnavailable,
			String summary,
			List<String> evidenceCitationIds,
			Instant createdAt
	) {
	}

	private record StoredInvestigation(InvestigationView view) {
	}
}
