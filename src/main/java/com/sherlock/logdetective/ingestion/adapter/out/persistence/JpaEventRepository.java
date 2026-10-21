package com.sherlock.logdetective.ingestion.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.EventLevel;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.port.out.EventRepository;

@Repository
@Transactional
public class JpaEventRepository implements EventRepository {

	private final SpringDataCanonicalEventJpaRepository events;

	public JpaEventRepository(SpringDataCanonicalEventJpaRepository events) {
		this.events = events;
	}

	@Override
	public CanonicalEvent save(CanonicalEvent event) {
		CanonicalEventEntity entity = events.findById(event.id()).orElseGet(CanonicalEventEntity::new);
		entity.setId(event.id());
		entity.setSourceId(event.sourceId().value());
		entity.setSourcePayloadId(event.sourcePayloadId());
		entity.setEventTimestamp(event.timestamp());
		entity.setIngestedAt(event.ingestedAt());
		entity.setLevel(event.level().name());
		entity.setMessage(event.message());
		entity.setLogger(event.logger().orElse(null));
		entity.setService(event.service().orElse(null));
		entity.setHost(event.host().orElse(null));
		entity.setTraceId(event.traceId().orElse(null));
		entity.setSpanId(event.spanId().orElse(null));
		entity.setAttributes(Map.copyOf(event.attributes()));
		return toDomain(events.saveAndFlush(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CanonicalEvent> findById(UUID id) {
		return events.findById(id).map(JpaEventRepository::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CanonicalEvent> findBySourcePayloadId(LogSourceId sourceId, String sourcePayloadId) {
		return events.findBySourceIdAndSourcePayloadId(sourceId.value(), sourcePayloadId)
				.map(JpaEventRepository::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CanonicalEvent> findBySourceId(LogSourceId sourceId) {
		return events.findBySourceIdOrderByEventTimestampAsc(sourceId.value()).stream()
				.map(JpaEventRepository::toDomain)
				.toList();
	}

	private static CanonicalEvent toDomain(CanonicalEventEntity entity) {
		return new CanonicalEvent(
				entity.getId(),
				new LogSourceId(entity.getSourceId()),
				entity.getSourcePayloadId(),
				entity.getEventTimestamp(),
				entity.getIngestedAt(),
				EventLevel.valueOf(entity.getLevel()),
				entity.getMessage(),
				Optional.ofNullable(entity.getLogger()),
				Optional.ofNullable(entity.getService()),
				Optional.ofNullable(entity.getHost()),
				Optional.ofNullable(entity.getTraceId()),
				Optional.ofNullable(entity.getSpanId()),
				entity.getAttributes() == null ? Map.of() : entity.getAttributes()
		);
	}
}
