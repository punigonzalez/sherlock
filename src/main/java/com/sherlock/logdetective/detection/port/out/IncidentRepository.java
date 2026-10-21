package com.sherlock.logdetective.detection.port.out;

import java.util.Optional;
import java.util.UUID;

import com.sherlock.logdetective.detection.domain.ErrorFingerprint;
import com.sherlock.logdetective.detection.domain.Incident;

public interface IncidentRepository {

	Incident save(Incident incident);

	Optional<Incident> findById(UUID id);

	Optional<Incident> findByFingerprint(ErrorFingerprint fingerprint);
}
