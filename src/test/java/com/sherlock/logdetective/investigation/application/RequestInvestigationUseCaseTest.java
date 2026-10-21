package com.sherlock.logdetective.investigation.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
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
import com.sherlock.logdetective.investigation.adapter.out.FakeAiInvestigator;
import com.sherlock.logdetective.investigation.adapter.out.NoOpAiInvestigator;
import com.sherlock.logdetective.investigation.domain.InvestigationReport;
import com.sherlock.logdetective.investigation.domain.InvestigationReport.InvestigationStatus;
import com.sherlock.logdetective.investigation.port.out.InvestigationReportRepository;
import com.sherlock.logdetective.investigation.port.out.InvestigationTools;
import com.sherlock.logdetective.investigation.port.out.InvestigationTools.ToolResult;
import com.sherlock.logdetective.shared.domain.PageResult;

class RequestInvestigationUseCaseTest {

	@Test
	void requestMarksNoOpAsUnavailableAndPersists() {
		Incident incident = sampleIncident();
		InMemoryIncidents incidents = new InMemoryIncidents(incident);
		InMemoryReports reports = new InMemoryReports();
		RequestInvestigationUseCase useCase = new RequestInvestigationUseCase(
				incidents,
				new EmptyEvidence(),
				new InvestigateIncidentUseCase(new NoOpAiInvestigator()),
				reports
		);

		Optional<RequestInvestigationUseCase.InvestigationView> view = useCase.request(incident.id());

		assertThat(view).isPresent();
		assertThat(view.get().status()).isEqualTo(InvestigationStatus.NOOP_UNAVAILABLE);
		assertThat(view.get().aiUnavailable()).isTrue();
		assertThat(view.get().summary()).contains("NoOp");
		assertThat(reports.byIncident.get(incident.id())).hasSize(1);
		assertThat(useCase.latest(incident.id())).isPresent();
		assertThat(incidents.byId.get(incident.id()).status()).isEqualTo(IncidentStatus.INVESTIGATING);
	}

	@Test
	void fakeInvestigatorCompletesWithTools() {
		Incident incident = sampleIncident();
		InMemoryIncidents incidents = new InMemoryIncidents(incident);
		InvestigationTools tools = new InvestigationTools() {
			@Override
			public ToolResult getIncidentDetails(UUID incidentId) {
				return ToolResult.ok("getIncidentDetails", "ok", List.of(Map.of("title", incident.title())));
			}

			@Override
			public ToolResult getRelatedErrors(UUID incidentId, int limit) {
				return ToolResult.ok("getRelatedErrors", "ok", List.of());
			}

			@Override
			public ToolResult getRecentEvents(int limit) {
				return ToolResult.ok("getRecentEvents", "ok", List.of());
			}

			@Override
			public ToolResult searchLogs(String query, int limit) {
				return ToolResult.ok("searchLogs", "ok", List.of());
			}

			@Override
			public ToolResult getTraceDetails(String traceId, int limit) {
				return ToolResult.notFound("getTraceDetails", "none");
			}

			@Override
			public ToolResult getServiceMetrics(String service) {
				return ToolResult.unsupported("getServiceMetrics", "n/a");
			}
		};
		RequestInvestigationUseCase useCase = new RequestInvestigationUseCase(
				incidents,
				new EmptyEvidence(),
				new InvestigateIncidentUseCase(new FakeAiInvestigator(tools, 8)),
				new InMemoryReports()
		);

		var view = useCase.request(incident.id()).orElseThrow();
		assertThat(view.status()).isEqualTo(InvestigationStatus.COMPLETED);
		assertThat(view.provider()).isEqualTo("fake");
		assertThat(view.toolCalls()).isNotEmpty();
		assertThat(view.aiUnavailable()).isFalse();
	}

	@Test
	void requestReturnsEmptyForUnknownIncident() {
		RequestInvestigationUseCase useCase = new RequestInvestigationUseCase(
				new InMemoryIncidents(null),
				new EmptyEvidence(),
				new InvestigateIncidentUseCase(new NoOpAiInvestigator()),
				new InMemoryReports()
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

	private static final class InMemoryReports implements InvestigationReportRepository {
		private final Map<UUID, List<InvestigationReport>> byIncident = new HashMap<>();

		@Override
		public InvestigationReport save(InvestigationReport report) {
			byIncident.computeIfAbsent(report.incidentId(), ignored -> new ArrayList<>()).add(report);
			return report;
		}

		@Override
		public Optional<InvestigationReport> findById(UUID id) {
			return byIncident.values().stream().flatMap(List::stream).filter(r -> r.id().equals(id)).findFirst();
		}

		@Override
		public Optional<InvestigationReport> findLatestByIncidentId(UUID incidentId) {
			List<InvestigationReport> list = byIncident.getOrDefault(incidentId, List.of());
			return list.isEmpty() ? Optional.empty() : Optional.of(list.getLast());
		}

		@Override
		public List<InvestigationReport> findByIncidentId(UUID incidentId) {
			return List.copyOf(byIncident.getOrDefault(incidentId, List.of()));
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
