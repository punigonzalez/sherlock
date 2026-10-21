package com.sherlock.logdetective.api.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase;
import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase.InvestigationView;
import com.sherlock.logdetective.investigation.domain.InvestigationReport.InvestigationStatus;

class InvestigationControllerWebMvcTest {

	private MockMvc mockMvc;
	private RequestInvestigationUseCase requestInvestigationUseCase;

	@BeforeEach
	void setUp() {
		requestInvestigationUseCase = mock(RequestInvestigationUseCase.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new InvestigationController(requestInvestigationUseCase))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void requestInvestigationReturnsHonestNoOp() throws Exception {
		UUID id = UUID.randomUUID();
		when(requestInvestigationUseCase.request(id)).thenReturn(Optional.of(new InvestigationView(
				UUID.randomUUID(),
				id,
				InvestigationStatus.NOOP_UNAVAILABLE,
				"noop",
				true,
				"AI investigation disabled (NoOp).",
				"",
				"NONE",
				List.of(),
				List.of(),
				Instant.parse("2026-10-20T15:00:00Z")
		)));

		mockMvc.perform(post("/api/v1/incidents/" + id + "/investigations"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("NOOP_UNAVAILABLE"))
				.andExpect(jsonPath("$.aiUnavailable").value(true))
				.andExpect(jsonPath("$.note").value("AI diagnosis unavailable or NoOp; deterministic detection remains valid."));
	}

	@Test
	void latestMissingReturnsNotFound() throws Exception {
		UUID id = UUID.randomUUID();
		when(requestInvestigationUseCase.latest(id)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/incidents/" + id + "/investigations/latest"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("no investigation report available for incident"));
	}
}
