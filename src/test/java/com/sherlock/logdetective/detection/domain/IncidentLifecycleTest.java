package com.sherlock.logdetective.detection.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sherlock.logdetective.ingestion.domain.EventLevel;

class IncidentLifecycleTest {

	@Test
	void observationOnResolvedIncidentReopens() {
		ErrorFingerprint fingerprint = new ErrorFingerprint("abc", "level=ERROR|msg=x");
		ErrorObservation first = observation(Instant.parse("2026-10-20T10:00:00Z"), "disk full");
		Incident incident = Incident.openNew(fingerprint, first, "disk full");
		incident.transitionTo(IncidentStatus.INVESTIGATING, Instant.parse("2026-10-20T10:05:00Z"), "triage");
		incident.transitionTo(IncidentStatus.RESOLVED, Instant.parse("2026-10-20T11:00:00Z"), "fixed");

		incident.applyObservation(observation(Instant.parse("2026-10-20T12:00:00Z"), "disk full"));

		assertThat(incident.status()).isEqualTo(IncidentStatus.REOPENED);
		assertThat(incident.occurrenceCount()).isEqualTo(2);
	}

	@Test
	void rejectsIllegalTransition() {
		ErrorFingerprint fingerprint = new ErrorFingerprint("abc", "level=ERROR|msg=x");
		Incident incident = Incident.openNew(
				fingerprint,
				observation(Instant.parse("2026-10-20T10:00:00Z"), "x"),
				"x"
		);

		assertThatThrownBy(() ->
				incident.transitionTo(IncidentStatus.REOPENED, Instant.parse("2026-10-20T10:01:00Z"), "bad")
		).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void groupsEvidenceRefsFromObservations() {
		ErrorFingerprint fingerprint = new ErrorFingerprint("abc", "level=ERROR|msg=x");
		Incident incident = Incident.openNew(
				fingerprint,
				new ErrorObservation(
						Instant.parse("2026-10-20T10:00:00Z"),
						EventLevel.ERROR,
						"x",
						Optional.empty(),
						Optional.empty(),
						Optional.empty(),
						Optional.of("ev-1"),
						Severity.HIGH,
						Map.of()
				),
				"x"
		);
		incident.applyObservation(new ErrorObservation(
				Instant.parse("2026-10-20T10:01:00Z"),
				EventLevel.ERROR,
				"x",
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.of("ev-2"),
				Severity.HIGH,
				Map.of()
		));

		assertThat(incident.evidenceRefs()).containsExactlyInAnyOrder("ev-1", "ev-2");
	}

	private static ErrorObservation observation(Instant at, String message) {
		return new ErrorObservation(
				at,
				EventLevel.ERROR,
				message,
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Severity.MEDIUM,
				Map.of()
		);
	}
}
