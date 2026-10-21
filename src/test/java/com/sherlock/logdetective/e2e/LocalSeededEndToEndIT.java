package com.sherlock.logdetective.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.sherlock.logdetective.correlation.application.QueryEvidenceUseCase;
import com.sherlock.logdetective.detection.application.QueryIncidentsUseCase;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.ingestion.adapter.out.file.FileLogSource;
import com.sherlock.logdetective.ingestion.application.IngestLogsUseCase;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase;
import com.sherlock.logdetective.investigation.domain.InvestigationReport.InvestigationStatus;
import com.sherlock.sherlock.SherlockApplication;

/**
 * Seeded local MVP path: file fixture → ingest → incidents → fake AI investigation → evidence refs.
 * Asserts fingerprinting prevents unlimited duplicate incidents on re-ingest.
 */
@SpringBootTest(classes = SherlockApplication.class)
@Testcontainers
@TestPropertySource(properties = {
		"logdetective.investigation.provider=fake",
		"logdetective.ingestion.enabled=false",
		"logdetective.gcp-logging.enabled=false",
		"logdetective.detection.threshold.min-error-count=2",
		"logdetective.detection.threshold.window-seconds=86400"
})
class LocalSeededEndToEndIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@TempDir
	Path tempDir;

	@Autowired
	private IngestLogsUseCase ingestLogsUseCase;

	@Autowired
	private QueryIncidentsUseCase queryIncidentsUseCase;

	@Autowired
	private QueryEvidenceUseCase queryEvidenceUseCase;

	@Autowired
	private RequestInvestigationUseCase requestInvestigationUseCase;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void fileToIncidentsToFakeInvestigation() throws Exception {
		Path log = tempDir.resolve("app.log");
		Files.copy(Path.of("src/test/resources/fixtures/sample-app.log"), log);

		FileLogSource source = new FileLogSource(new LogSourceId("e2e-app"), log);
		var first = ingestLogsUseCase.ingest(source, 100);
		assertThat(first.accepted()).isGreaterThanOrEqualTo(3);
		assertThat(first.errorsRoutedToIncidents()).isGreaterThanOrEqualTo(2);

		Integer incidentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM incidents", Integer.class);
		assertThat(incidentCount).isGreaterThanOrEqualTo(1);

		var page = queryIncidentsUseCase.list(0, 50, java.util.Optional.empty());
		assertThat(page.items()).isNotEmpty();
		Incident incident = page.items().getFirst();

		var investigation = requestInvestigationUseCase.request(incident.id()).orElseThrow();
		assertThat(investigation.status()).isEqualTo(InvestigationStatus.COMPLETED);
		assertThat(investigation.provider()).isEqualTo("fake");
		assertThat(investigation.toolCalls()).isNotEmpty();
		assertThat(investigation.reportId()).isNotNull();

		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM investigation_reports WHERE incident_id = ?",
				Integer.class,
				incident.id()
		)).isEqualTo(1);

		var latest = requestInvestigationUseCase.latest(incident.id());
		assertThat(latest).isPresent();
		assertThat(latest.get().reportId()).isEqualTo(investigation.reportId());

		// evidence list is well-defined (may be empty if only fingerprint incidents)
		assertThat(queryEvidenceUseCase.forIncident(incident.id())).isNotNull();

		// Re-ingest same file: checkpoint + fingerprinting → no unbounded new incidents
		var second = ingestLogsUseCase.ingest(source, 100);
		assertThat(second.accepted()).isZero();
		Integer after = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM incidents", Integer.class);
		assertThat(after).isEqualTo(incidentCount);

		// Append a duplicate-signature error; should group, not explode incident count unboundedly
		Files.writeString(
				log,
				Files.readString(log)
						+ "\n2026-10-20 12:05:00,000 ERROR [worker] com.example.Payments - Payment failed for order 1 id="
						+ UUID.randomUUID()
						+ "\n"
		);
		var third = ingestLogsUseCase.ingest(source, 100);
		assertThat(third.accepted()).isGreaterThanOrEqualTo(1);
		Integer afterAppend = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM incidents", Integer.class);
		assertThat(afterAppend).isLessThanOrEqualTo(incidentCount + 2);
	}
}
