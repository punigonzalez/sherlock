package com.sherlock.logdetective.ingestion.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCanonicalEventJpaRepository extends JpaRepository<CanonicalEventEntity, UUID> {

	Optional<CanonicalEventEntity> findBySourceIdAndSourcePayloadId(String sourceId, String sourcePayloadId);

	List<CanonicalEventEntity> findBySourceIdOrderByEventTimestampAsc(String sourceId);
}
