package com.sherlock.logdetective.ingestion.port.out;

import java.util.Optional;

import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.port.out.LogSource.FetchCursor;

public interface SourceCheckpointStore {

	Optional<FetchCursor> load(LogSourceId sourceId);

	void save(LogSourceId sourceId, FetchCursor cursor);
}
