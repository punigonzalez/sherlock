package com.sherlock.logdetective.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import com.sherlock.logdetective.correlation.application.QueryEvidenceUseCase;
import com.sherlock.logdetective.detection.application.QueryIncidentsUseCase;
import com.sherlock.logdetective.detection.domain.ErrorFingerprint;
import com.sherlock.logdetective.detection.domain.ErrorObservation;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.Severity;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase.ValidationResult;
import com.sherlock.logdetective.ingestion.domain.EventLevel;
import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase;
import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase.InvestigationView;
import com.sherlock.logdetective.investigation.domain.InvestigationReport.InvestigationStatus;
import com.sherlock.logdetective.shared.domain.PageResult;

class CliFacadeTest {

	@Test
	void validateSourcePrintsResult() {
		ManageFileSourcesUseCase sources = mock(ManageFileSourcesUseCase.class);
		when(sources.validate(eq("app"), eq("./logs/app.log"), eq("UTF-8")))
				.thenReturn(new ValidationResult(true, "app", "/tmp/logs/app.log", "OK", "ok", null));

		CliFacade facade = new CliFacade(
				sources,
				mock(QueryIncidentsUseCase.class),
				mock(QueryEvidenceUseCase.class),
				mock(RequestInvestigationUseCase.class)
		);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int code = facade.execute(
				new DefaultApplicationArguments(
						"--cli=validate-source",
						"--id=app",
						"--path=./logs/app.log"
				),
				new PrintStream(out),
				new PrintStream(new ByteArrayOutputStream())
		);

		assertThat(code).isZero();
		assertThat(out.toString()).contains("valid=true").contains("id=app");
	}

	@Test
	void listIncidentsUsesQueryUseCase() {
		Incident incident = sampleIncident();
		QueryIncidentsUseCase query = mock(QueryIncidentsUseCase.class);
		when(query.list(anyInt(), anyInt(), any()))
				.thenReturn(PageResult.of(List.of(incident), 0, 20, 1));

		CliFacade facade = new CliFacade(
				mock(ManageFileSourcesUseCase.class),
				query,
				mock(QueryEvidenceUseCase.class),
				mock(RequestInvestigationUseCase.class)
		);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int code = facade.execute(
				new DefaultApplicationArguments("--cli=list-incidents"),
				new PrintStream(out),
				new PrintStream(new ByteArrayOutputStream())
		);

		assertThat(code).isZero();
		assertThat(out.toString()).contains(incident.id().toString());
	}

	@Test
	void requestInvestigationShowsNoOpNote() {
		UUID id = UUID.randomUUID();
		RequestInvestigationUseCase investigation = mock(RequestInvestigationUseCase.class);
		when(investigation.request(id)).thenReturn(Optional.of(new InvestigationView(
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

		CliFacade facade = new CliFacade(
				mock(ManageFileSourcesUseCase.class),
				mock(QueryIncidentsUseCase.class),
				mock(QueryEvidenceUseCase.class),
				investigation
		);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int code = facade.execute(
				new DefaultApplicationArguments("--cli=request-investigation", "--id=" + id),
				new PrintStream(out),
				new PrintStream(new ByteArrayOutputStream())
		);

		assertThat(code).isZero();
		assertThat(out.toString()).contains("aiUnavailable=true").contains("NoOp");
	}

	@Test
	void unknownCommandReturnsUsage() {
		CliFacade facade = new CliFacade(
				mock(ManageFileSourcesUseCase.class),
				mock(QueryIncidentsUseCase.class),
				mock(QueryEvidenceUseCase.class),
				mock(RequestInvestigationUseCase.class)
		);

		ByteArrayOutputStream err = new ByteArrayOutputStream();
		int code = facade.execute(
				new DefaultApplicationArguments("--cli=nope"),
				new PrintStream(new ByteArrayOutputStream()),
				new PrintStream(err)
		);

		assertThat(code).isEqualTo(2);
		assertThat(err.toString()).contains("Unknown CLI command");
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
