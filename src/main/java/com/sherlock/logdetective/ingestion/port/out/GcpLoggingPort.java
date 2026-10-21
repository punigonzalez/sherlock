package com.sherlock.logdetective.ingestion.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Narrow port over Cloud Logging list APIs for testability (no Pub/Sub).
 */
public interface GcpLoggingPort {

	Page listEntries(String filter, String pageToken, int pageSize);

	void ping();

	record LogEntryView(
			String insertId,
			Instant timestamp,
			String severity,
			String logName,
			String textPayload,
			Map<String, String> labels
	) {
	}

	record Page(List<LogEntryView> entries, String nextPageToken) {
		public Page {
			entries = entries == null ? List.of() : List.copyOf(entries);
			nextPageToken = nextPageToken == null ? "" : nextPageToken;
		}

		public boolean hasNext() {
			return nextPageToken != null && !nextPageToken.isBlank();
		}
	}
}
