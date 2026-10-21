package com.sherlock.logdetective.ingestion.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.port.out.LogSource.FetchCursor;
import com.sherlock.logdetective.ingestion.port.out.SourceCheckpointStore;

@Repository
@Transactional
public class JpaSourceCheckpointStore implements SourceCheckpointStore {

	private final SpringDataCheckpointJpaRepository checkpoints;

	public JpaSourceCheckpointStore(SpringDataCheckpointJpaRepository checkpoints) {
		this.checkpoints = checkpoints;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<FetchCursor> load(LogSourceId sourceId) {
		return checkpoints.findById(sourceId.value())
				.map(entity -> new FetchCursor(entity.getCursorOpaque()));
	}

	@Override
	public void save(LogSourceId sourceId, FetchCursor cursor) {
		SourceCheckpointEntity entity = checkpoints.findById(sourceId.value())
				.orElseGet(() -> new SourceCheckpointEntity(sourceId.value(), cursor.opaque(), Instant.now()));
		entity.setCursorOpaque(cursor.opaque());
		entity.setUpdatedAt(Instant.now());
		checkpoints.saveAndFlush(entity);
	}
}
