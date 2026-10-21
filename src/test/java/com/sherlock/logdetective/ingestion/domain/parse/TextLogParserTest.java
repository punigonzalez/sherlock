package com.sherlock.logdetective.ingestion.domain.parse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.sherlock.logdetective.ingestion.domain.EventLevel;

class TextLogParserTest {

	private final TextLogParser parser = new TextLogParser();

	@Test
	void parsesConventionalLine() {
		ParseResult result = parser.parse(
				"2026-10-20 12:00:01,100 ERROR [worker] com.example.Payments - Payment failed",
				Instant.parse("2026-10-20T00:00:00Z")
		);
		assertThat(result.success()).isTrue();
		assertThat(result.fields().orElseThrow().level()).isEqualTo(EventLevel.ERROR);
		assertThat(result.fields().orElseThrow().logger()).contains("com.example.Payments");
		assertThat(result.fields().orElseThrow().message()).contains("Payment failed");
	}

	@Test
	void freeTextIsNotDiscarded() {
		ParseResult result = parser.parse("totally unstructured", Instant.now());
		assertThat(result.success()).isTrue();
		assertThat(result.fields().orElseThrow().level()).isEqualTo(EventLevel.UNKNOWN);
		assertThat(result.fields().orElseThrow().message()).isEqualTo("totally unstructured");
	}
}
