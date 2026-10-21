package com.sherlock.logdetective.ingestion.application;

public record IngestBatchResult(
		int fetched,
		int accepted,
		int duplicates,
		int parseFailures,
		int errorsRoutedToIncidents
) {
}
