package com.sherlock.logdetective.api.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sherlock.logdetective.correlation.application.QueryEvidenceUseCase;
import com.sherlock.logdetective.correlation.domain.CorrelationStrength;
import com.sherlock.logdetective.correlation.domain.Evidence;
import com.sherlock.logdetective.correlation.domain.EvidenceKind;
import com.sherlock.logdetective.detection.application.QueryIncidentsUseCase;
import com.sherlock.logdetective.detection.domain.ErrorFingerprint;
import com.sherlock.logdetective.detection.domain.ErrorObservation;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.Severity;
import com.sherlock.logdetective.ingestion.domain.EventLevel;
import com.sherlock.logdetective.shared.domain.PageResult;

class IncidentControllerWebMvcTest {

	private MockMvc mockMvc;
	private QueryIncidentsUseCase queryIncidentsUseCase;
	private QueryEvidenceUseCase queryEvidenceUseCase;

	@BeforeEach
	void setUp() {
		queryIncidentsUseCase = mock(QueryIncidentsUseCase.class);
		queryEvidenceUseCase = mock(QueryEvidenceUseCase.class);
		mockMvc = MockMvcBuilders.standaloneSetup(
						new IncidentController(queryIncidentsUseCase, queryEvidenceUseCase)
				)
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void listIncidents() throws Exception {
		Incident incident = sampleIncident();
		when(queryIncidentsUseCase.list(eq(0), eq(20), any()))
				.thenReturn(PageResult.of(List.of(incident), 0, 20, 1));

		mockMvc.perform(get("/api/v1/incidents"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.items[0].id").value(incident.id().toString()))
				.andExpect(jsonPath("$.items[0].status").value("OPEN"));
	}

	@Test
	void getIncidentNotFound() throws Exception {
		UUID id = UUID.randomUUID();
		when(queryIncidentsUseCase.get(id)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/incidents/" + id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("incident not found"));
	}

	@Test
	void listEvidence() throws Exception {
		Incident incident = sampleIncident();
		Evidence evidence = Evidence.of(
				EvidenceKind.TRACE_CORRELATION,
				CorrelationStrength.STRONG,
				"shared traceId across services (not causal)",
				"trace cluster",
				java.util.Set.of(),
				Map.of("traceId", "t-1")
		);
		when(queryIncidentsUseCase.get(incident.id())).thenReturn(Optional.of(incident));
		when(queryEvidenceUseCase.forIncident(incident.id())).thenReturn(List.of(evidence));

		mockMvc.perform(get("/api/v1/incidents/" + incident.id() + "/evidence"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].kind").value("TRACE_CORRELATION"))
				.andExpect(jsonPath("$[0].strength").value("STRONG"));
	}

	@Test
	void invalidStatusIsBadRequest() throws Exception {
		mockMvc.perform(get("/api/v1/incidents").param("status", "NOPE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("unknown status: NOPE"));
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
}
