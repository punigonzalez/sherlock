package com.sherlock.logdetective.ingestion.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;

/**
 * Outbound port for persisting canonical events.
 */
public interface EventRepository {

	CanonicalEvent save(CanonicalEvent event);

	Optional<CanonicalEvent> findById(UUID id);

	Optional<CanonicalEvent> findBySourcePayloadId(LogSourceId sourceId, String sourcePayloadId);

	List<CanonicalEvent> findBySourceId(LogSourceId sourceId);

	List<CanonicalEvent> findBetween(Instant fromInclusive, Instant toInclusive);
}
