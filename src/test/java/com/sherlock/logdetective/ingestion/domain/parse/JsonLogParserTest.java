package com.sherlock.logdetective.ingestion.domain.parse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.sherlock.logdetective.ingestion.domain.EventLevel;

class JsonLogParserTest {

	private final JsonLogParser parser = new JsonLogParser();

	@Test
	void parsesStructuredJson() {
		ParseResult result = parser.parse(
				"""
						{"timestamp":"2026-10-20T12:00:02Z","level":"ERROR","service":"billing","message":"timeout","logger":"net.Client"}
						""",
				Instant.parse("2026-10-20T00:00:00Z")
		);

		assertThat(result.success()).isTrue();
		ParsedLogFields fields = result.fields().orElseThrow();
		assertThat(fields.level()).isEqualTo(EventLevel.ERROR);
		assertThat(fields.service()).contains("billing");
		assertThat(fields.message()).isEqualTo("timeout");
	}

	@Test
	void missingMessageIsExplicitFailure() {
		ParseResult result = parser.parse("{\"level\":\"INFO\"}", Instant.now());
		assertThat(result.success()).isFalse();
		assertThat(result.failureReason()).contains("json-missing-message");
	}
}
