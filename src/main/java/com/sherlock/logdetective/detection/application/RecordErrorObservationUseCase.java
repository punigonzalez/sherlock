package com.sherlock.logdetective.detection.application;

import java.util.Objects;

import com.sherlock.logdetective.detection.domain.ErrorFingerprint;
import com.sherlock.logdetective.detection.domain.ErrorObservation;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.fingerprint.ErrorFingerprintCalculator;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;

/**
 * Creates an incident or updates the existing one grouped by error fingerprint.
 */
public final class RecordErrorObservationUseCase {

	private final IncidentRepository incidentRepository;
	private final ErrorFingerprintCalculator fingerprintCalculator;

	public RecordErrorObservationUseCase(
			IncidentRepository incidentRepository,
			ErrorFingerprintCalculator fingerprintCalculator
	) {
		this.incidentRepository = Objects.requireNonNull(incidentRepository, "incidentRepository");
		this.fingerprintCalculator = Objects.requireNonNull(fingerprintCalculator, "fingerprintCalculator");
	}

	public Incident execute(ErrorObservation observation) {
		Objects.requireNonNull(observation, "observation");

		ErrorFingerprint fingerprint = fingerprintCalculator.calculate(
				observation.level(),
				observation.message(),
				observation.logger(),
				observation.service()
		);

		Incident incident = incidentRepository.findByFingerprint(fingerprint)
				.map(existing -> {
					existing.applyObservation(observation);
					return existing;
				})
				.orElseGet(() -> Incident.openNew(fingerprint, observation, titleFor(observation)));

		return incidentRepository.save(incident);
	}

	private static String titleFor(ErrorObservation observation) {
		String message = observation.message().trim();
		return message.length() <= 120 ? message : message.substring(0, 117) + "...";
	}
}
