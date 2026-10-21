package com.sherlock.logdetective.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.sherlock.logdetective.ingestion.adapter.out.file.FileLogSource;
import com.sherlock.logdetective.ingestion.application.IngestLogsUseCase;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.port.out.SourceCheckpointStore;
import com.sherlock.sherlock.SherlockApplication;

/**
 * Restart behavior: checkpoint survives and prevents re-processing the same bytes.
 */
@SpringBootTest(classes = SherlockApplication.class)
@Testcontainers
class CheckpointRestartIT {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@TempDir
	Path tempDir;

	@Autowired
	private IngestLogsUseCase ingestLogsUseCase;

	@Autowired
	private SourceCheckpointStore checkpointStore;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void checkpointPreventsDuplicateAcceptOnSimulatedRestart() throws Exception {
		Path log = tempDir.resolve("app.log");
		Files.writeString(log, """
				2026-10-20 12:00:00,001 INFO [main] com.example.App - started
				2026-10-20 12:00:01,100 ERROR [worker] com.example.Payments - Payment failed id=550e8400-e29b-41d4-a716-446655440000
				""");

		LogSourceId id = new LogSourceId("restart-src");
		FileLogSource firstProcess = new FileLogSource(id, log);
		assertThat(ingestLogsUseCase.ingest(firstProcess, 50).accepted()).isGreaterThanOrEqualTo(1);
		assertThat(checkpointStore.load(id)).isPresent();

		Integer eventsAfterFirst = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM canonical_events WHERE source_id = ?",
				Integer.class,
				id.value()
		);

		// Simulate process restart with a new source instance sharing the same id + path.
		FileLogSource afterRestart = new FileLogSource(id, log);
		assertThat(ingestLogsUseCase.ingest(afterRestart, 50).accepted()).isZero();
		Integer eventsAfterSecond = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM canonical_events WHERE source_id = ?",
				Integer.class,
				id.value()
		);
		assertThat(eventsAfterSecond).isEqualTo(eventsAfterFirst);
	}
}
