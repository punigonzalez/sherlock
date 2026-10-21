package com.sherlock.logdetective.ingestion.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.domain.SourceType;

/**
 * Outbound port for pulling raw log records from an external source.
 */
public interface LogSource {

	LogSourceId id();

	SourceType type();

	List<RawLogRecord> fetch(FetchCursor cursor, int limit);

	FetchCursor advance(FetchCursor cursor, List<RawLogRecord> consumed);

	record RawLogRecord(
			String sourcePayloadId,
			Instant observedAt,
			String rawBody,
			Map<String, String> headers
	) {
	}

	record FetchCursor(String opaque) {
		public static FetchCursor start() {
			return new FetchCursor("");
		}
	}
}
