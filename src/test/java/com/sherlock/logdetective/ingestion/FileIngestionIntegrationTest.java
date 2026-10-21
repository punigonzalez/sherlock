package com.sherlock.logdetective.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.sherlock.logdetective.ingestion.adapter.out.file.FileLogSource;
import com.sherlock.logdetective.ingestion.application.IngestBatchResult;
import com.sherlock.logdetective.ingestion.application.IngestLogsUseCase;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.sherlock.SherlockApplication;

@SpringBootTest(classes = SherlockApplication.class)
@Testcontainers
@Transactional
class FileIngestionIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@TempDir
	Path tempDir;

	@Autowired
	private IngestLogsUseCase ingestLogsUseCase;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void persistsEventsCheckpointsAndIncidentsFromFile() throws Exception {
		Path log = tempDir.resolve("app.log");
		Files.copy(Path.of("src/test/resources/fixtures/sample-app.log"), log);

		FileLogSource source = new FileLogSource(new LogSourceId("it-app-log"), log);
		IngestBatchResult result = ingestLogsUseCase.ingest(source, 100);

		assertThat(result.accepted()).isGreaterThanOrEqualTo(3);
		assertThat(result.errorsRoutedToIncidents()).isGreaterThanOrEqualTo(2);

		Integer events = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM canonical_events", Integer.class);
		Integer checkpoints = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM source_checkpoints", Integer.class);
		Integer incidents = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM incidents", Integer.class);
		String schema = jdbcTemplate.queryForObject("SELECT schema_version FROM app_schema_meta WHERE id = 1", String.class);

		assertThat(events).isGreaterThanOrEqualTo(3);
		assertThat(checkpoints).isEqualTo(1);
		assertThat(incidents).isGreaterThanOrEqualTo(1);
		assertThat(schema).isEqualTo("phase-3-ingestion");

		IngestBatchResult again = ingestLogsUseCase.ingest(source, 100);
		assertThat(again.accepted()).isZero();
	}
}
