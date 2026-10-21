package com.sherlock.logdetective.ingestion.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.domain.SourceHealth;
import com.sherlock.logdetective.ingestion.domain.SourceType;

/**
 * Outbound port for pulling raw log records from an external source.
 * Lifecycle: {@link #validate()} → {@link #start()} → fetch/advance → {@link #stop()}.
 */
public interface LogSource {

	LogSourceId id();

	SourceType type();

	/** Fail fast if configuration/path is unusable. */
	void validate();

	void start();

	void stop();

	boolean isRunning();

	SourceHealth health();

	List<RawLogRecord> fetch(FetchCursor cursor, int limit);

	/**
	 * Returns the cursor to persist after a batch was safely accepted.
	 * Implementations may encode offsets / rotation tokens in {@link FetchCursor#opaque()}.
	 */
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

		public boolean isStart() {
			return opaque == null || opaque.isBlank();
		}
	}
}
