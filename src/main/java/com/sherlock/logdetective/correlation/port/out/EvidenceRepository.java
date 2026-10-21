package com.sherlock.logdetective.correlation.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sherlock.logdetective.correlation.domain.Evidence;
import com.sherlock.logdetective.correlation.domain.EvidenceId;

public interface EvidenceRepository {

	Evidence save(Evidence evidence);

	Optional<Evidence> findById(EvidenceId id);

	void linkToIncident(UUID incidentId, Evidence evidence);

	List<Evidence> findByIncidentId(UUID incidentId);
}
