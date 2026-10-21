package com.sherlock.logdetective.detection.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.sherlock.sherlock.SherlockApplication;
import com.sherlock.logdetective.detection.application.RecordErrorObservationUseCase;
import com.sherlock.logdetective.detection.domain.ErrorObservation;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.IncidentStatus;
import com.sherlock.logdetective.detection.domain.Severity;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;
import com.sherlock.logdetective.ingestion.domain.EventLevel;

@SpringBootTest(classes = SherlockApplication.class)
@Testcontainers
@Transactional
class JpaIncidentRepositoryIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	private RecordErrorObservationUseCase recordErrorObservationUseCase;

	@Autowired
	private IncidentRepository incidentRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void endToEndGroupingAndPersistence() {
		Incident first = recordErrorObservationUseCase.execute(observation(
				Instant.parse("2026-10-20T19:00:00Z"),
				"Timeout talking to redis aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
				Optional.of("ev-a")
		));
		Incident second = recordErrorObservationUseCase.execute(observation(
				Instant.parse("2026-10-20T19:05:00Z"),
				"Timeout talking to redis 11111111-2222-3333-4444-555555555555",
				Optional.of("ev-b")
		));

		assertThat(second.id()).isEqualTo(first.id());
		assertThat(second.occurrenceCount()).isEqualTo(2);
		assertThat(second.evidenceRefs()).containsExactlyInAnyOrder("ev-a", "ev-b");

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM error_fingerprints", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM incidents", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM incident_evidence_links", Integer.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT schema_version FROM app_schema_meta WHERE id = 1",
				String.class
		)).isEqualTo("phase-6-investigation");
	}

	@Test
	void resolvedIncidentReopensOnNewObservationPersisted() {
		Incident open = recordErrorObservationUseCase.execute(observation(
				Instant.parse("2026-10-20T20:00:00Z"),
				"connection reset by peer deadbeefcafebabe",
				Optional.of("ev-r1")
		));
		open.transitionTo(IncidentStatus.RESOLVED, Instant.parse("2026-10-20T20:10:00Z"), "done");
		incidentRepository.save(open);

		Incident reopened = recordErrorObservationUseCase.execute(observation(
				Instant.parse("2026-10-20T21:00:00Z"),
				"connection reset by peer 0011223344556677",
				Optional.of("ev-r2")
		));

		assertThat(reopened.id()).isEqualTo(open.id());
		assertThat(reopened.status()).isEqualTo(IncidentStatus.REOPENED);
		assertThat(reopened.occurrenceCount()).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT status FROM incidents WHERE id = ?",
				String.class,
				reopened.id()
		)).isEqualTo("REOPENED");
	}

	private static ErrorObservation observation(Instant at, String message, Optional<String> evidence) {
		return new ErrorObservation(
				at,
				EventLevel.ERROR,
				message,
				Optional.of("net.Client"),
				Optional.of("edge"),
				Optional.empty(),
				evidence,
				Severity.HIGH,
				Map.of("source", "test")
		);
	}
}
