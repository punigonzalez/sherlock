package com.sherlock.logdetective.ingestion.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;

/**
 * Outbound port for persisting canonical events. Adapter in a later phase.
 */
public interface EventRepository {

	CanonicalEvent save(CanonicalEvent event);

	Optional<CanonicalEvent> findById(UUID id);

	List<CanonicalEvent> findBySourceId(LogSourceId sourceId);
}
