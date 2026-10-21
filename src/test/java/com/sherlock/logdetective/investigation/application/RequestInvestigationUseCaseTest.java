package com.sherlock.logdetective.investigation.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sherlock.logdetective.correlation.domain.Evidence;
import com.sherlock.logdetective.correlation.domain.EvidenceId;
import com.sherlock.logdetective.correlation.port.out.EvidenceRepository;
import com.sherlock.logdetective.detection.domain.ErrorFingerprint;
import com.sherlock.logdetective.detection.domain.ErrorObservation;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.IncidentStatus;
import com.sherlock.logdetective.detection.domain.Severity;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;
import com.sherlock.logdetective.ingestion.domain.EventLevel;
import com.sherlock.logdetective.investigation.adapter.out.NoOpAiInvestigator;
import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase.InvestigationStatus;
import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase.InvestigationView;
import com.sherlock.logdetective.shared.domain.PageResult;

class RequestInvestigationUseCaseTest {

	@Test
	void requestMarksNoOpAsUnavailable() {
		Incident incident = sampleIncident();
		InMemoryIncidents incidents = new InMemoryIncidents(incident);
		RequestInvestigationUseCase useCase = new RequestInvestigationUseCase(
				incidents,
				new EmptyEvidence(),
				new InvestigateIncidentUseCase(new NoOpAiInvestigator()),
				new NoOpAiInvestigator()
		);

		Optional<InvestigationView> view = useCase.request(incident.id());

		assertThat(view).isPresent();
		assertThat(view.get().status()).isEqualTo(InvestigationStatus.NOOP_UNAVAILABLE);
		assertThat(view.get().aiUnavailable()).isTrue();
		assertThat(view.get().summary()).contains("NoOp");
		assertThat(useCase.latest(incident.id())).isPresent();
	}

	@Test
	void requestReturnsEmptyForUnknownIncident() {
		RequestInvestigationUseCase useCase = new RequestInvestigationUseCase(
				new InMemoryIncidents(null),
				new EmptyEvidence(),
				new InvestigateIncidentUseCase(new NoOpAiInvestigator()),
				new NoOpAiInvestigator()
		);

		assertThat(useCase.request(UUID.randomUUID())).isEmpty();
	}

	private static Incident sampleIncident() {
		ErrorObservation observation = new ErrorObservation(
				Instant.parse("2026-10-20T15:00:00Z"),
				EventLevel.ERROR,
				"boom",
				Optional.of("Main"),
				Optional.of("api"),
				Optional.empty(),
				Optional.empty(),
				Severity.HIGH,
				Map.of()
		);
		return Incident.openNew(new ErrorFingerprint("fp1", "boom"), observation, "boom");
	}

	private static final class InMemoryIncidents implements IncidentRepository {
		private final Map<UUID, Incident> byId = new HashMap<>();

		InMemoryIncidents(Incident seed) {
			if (seed != null) {
				byId.put(seed.id(), seed);
			}
		}

		@Override
		public Incident save(Incident incident) {
			byId.put(incident.id(), incident);
			return incident;
		}

		@Override
		public Optional<Incident> findById(UUID id) {
			return Optional.ofNullable(byId.get(id));
		}

		@Override
		public Optional<Incident> findByFingerprint(ErrorFingerprint fingerprint) {
			return Optional.empty();
		}

		@Override
		public PageResult<Incident> findPage(int page, int size, Optional<IncidentStatus> status) {
			return PageResult.of(List.of(), page, size, 0);
		}
	}

	private static final class EmptyEvidence implements EvidenceRepository {
		@Override
		public Evidence save(Evidence evidence) {
			return evidence;
		}

		@Override
		public Optional<Evidence> findById(EvidenceId id) {
			return Optional.empty();
		}

		@Override
		public void linkToIncident(UUID incidentId, Evidence evidence) {
		}

		@Override
		public List<Evidence> findByIncidentId(UUID incidentId) {
			return List.of();
		}
	}
}
