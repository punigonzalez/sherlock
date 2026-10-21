package com.sherlock.logdetective.ingestion.domain.parse;

import java.time.Instant;

public interface LogParser {

	ParseResult parse(String rawBody, Instant fallbackTimestamp);
}
