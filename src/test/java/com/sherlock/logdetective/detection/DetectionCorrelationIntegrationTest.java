package com.sherlock.logdetective.detection;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.sherlock.logdetective.detection.application.EvaluateDetectionsUseCase;
import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.EventLevel;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.port.out.EventRepository;
import com.sherlock.sherlock.SherlockApplication;

@SpringBootTest(classes = SherlockApplication.class)
@Testcontainers
@Transactional
@TestPropertySource(properties = {
		"logdetective.detection.threshold.min-error-count=3",
		"logdetective.detection.threshold.window-seconds=120",
		"logdetective.detection.spike.min-error-count=100",
		"logdetective.detection.correlation.temporal-window-seconds=60"
})
class DetectionCorrelationIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private EvaluateDetectionsUseCase evaluateDetectionsUseCase;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void persistsThresholdFindingEvidenceAndSchema() {
		Instant now = Instant.parse("2026-10-21T15:00:00Z");
		for (int i = 0; i < 3; i++) {
			eventRepository.save(event(now.minusSeconds(10 - i), "billing", Optional.empty(), "timeout " + i));
		}
		// temporal pair across services with shared token
		eventRepository.save(event(now.minusSeconds(4), "checkout", Optional.empty(), "payment timeout A"));
		eventRepository.save(event(now.minusSeconds(2), "payments", Optional.empty(), "payment timeout B"));
		// trace correlation
		eventRepository.save(event(now.minusSeconds(3), "edge", Optional.of("tr-9"), "boom"));
		eventRepository.save(event(now.minusSeconds(2), "core", Optional.of("tr-9"), "related", EventLevel.INFO));

		var result = evaluateDetectionsUseCase.evaluate(now);

		assertThat(result.findings()).isGreaterThanOrEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM evidence", Integer.class)).isGreaterThanOrEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM incident_evidence_links WHERE reason IS NOT NULL",
				Integer.class
		)).isGreaterThanOrEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT schema_version FROM app_schema_meta WHERE id = 1",
				String.class
		)).isEqualTo("phase-6-investigation");
	}

	private static CanonicalEvent event(Instant ts, String service, Optional<String> trace, String message) {
		return event(ts, service, trace, message, EventLevel.ERROR);
	}

	private static CanonicalEvent event(
			Instant ts,
			String service,
			Optional<String> trace,
			String message,
			EventLevel level
	) {
		return new CanonicalEvent(
				UUID.randomUUID(),
				new LogSourceId("it"),
				UUID.randomUUID().toString(),
				ts,
				ts,
				level,
				message,
				Optional.empty(),
				Optional.of(service),
				Optional.empty(),
				trace,
				Optional.empty(),
				Map.of()
		);
	}
}
