package com.sherlock.logdetective.api.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.sherlock.logdetective.ingestion.application.IngestBatchResult;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase.SourceView;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase.ValidationResult;

class SourceControllerWebMvcTest {

	private MockMvc mockMvc;
	private ManageFileSourcesUseCase manageFileSourcesUseCase;

	@BeforeEach
	void setUp() {
		manageFileSourcesUseCase = mock(ManageFileSourcesUseCase.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new SourceController(manageFileSourcesUseCase))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void listSources() throws Exception {
		when(manageFileSourcesUseCase.list()).thenReturn(List.of(
				new SourceView("app", "FILE", "/logs/app.log", true, "OK", "ready")
		));

		mockMvc.perform(get("/api/v1/sources"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("app"))
				.andExpect(jsonPath("$[0].healthStatus").value("OK"));
	}

	@Test
	void validateRejectsBlankBody() throws Exception {
		mockMvc.perform(post("/api/v1/sources/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"id\":\"\",\"path\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void validateReturnsResult() throws Exception {
		when(manageFileSourcesUseCase.validate(eq("app"), eq("./logs/app.log"), isNull()))
				.thenReturn(new ValidationResult(true, "app", "/abs/logs/app.log", "OK", "ok", null));

		mockMvc.perform(post("/api/v1/sources/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"id\":\"app\",\"path\":\"./logs/app.log\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valid").value(true))
				.andExpect(jsonPath("$.sourceId").value("app"));
	}

	@Test
	void ingestOnce() throws Exception {
		when(manageFileSourcesUseCase.ingestOnce("app"))
				.thenReturn(new IngestBatchResult(3, 2, 1, 0, 1));

		mockMvc.perform(post("/api/v1/sources/app/ingest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fetched").value(3))
				.andExpect(jsonPath("$.accepted").value(2));
	}

	@Test
	void healthNotFound() throws Exception {
		when(manageFileSourcesUseCase.health("missing")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/sources/missing/health"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("source not found"));
	}
}
