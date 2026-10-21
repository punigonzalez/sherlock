package com.sherlock.logdetective.ingestion.domain.parse;

import java.time.Instant;
import java.util.Objects;

/**
 * Tries JSON when the payload looks like JSON; otherwise conventional text.
 * JSON parse failures fall through to text so events are never silently dropped.
 */
public final class CompositeLogParser implements LogParser {

	private final JsonLogParser jsonLogParser;
	private final TextLogParser textLogParser;

	public CompositeLogParser() {
		this(new JsonLogParser(), new TextLogParser());
	}

	public CompositeLogParser(JsonLogParser jsonLogParser, TextLogParser textLogParser) {
		this.jsonLogParser = Objects.requireNonNull(jsonLogParser);
		this.textLogParser = Objects.requireNonNull(textLogParser);
	}

	@Override
	public ParseResult parse(String rawBody, Instant fallbackTimestamp) {
		String trimmed = rawBody == null ? "" : rawBody.trim();
		if (trimmed.startsWith("{")) {
			ParseResult json = jsonLogParser.parse(rawBody, fallbackTimestamp);
			if (json.success()) {
				return json;
			}
			ParseResult text = textLogParser.parse(rawBody, fallbackTimestamp);
			if (text.success()) {
				// Preserve that JSON was attempted and failed.
				return ParseResult.ok(withParseFailureHint(text.fields().orElseThrow(), json.failureReason().orElse("json-failed")));
			}
			return ParseResult.failure(json.failureReason().orElse("json-failed"));
		}
		return textLogParser.parse(rawBody, fallbackTimestamp);
	}

	private static ParsedLogFields withParseFailureHint(ParsedLogFields fields, String reason) {
		java.util.HashMap<String, String> attrs = new java.util.HashMap<>(fields.attributes());
		attrs.put("json_parse_failed", "true");
		attrs.put("json_parse_failure_reason", reason);
		return new ParsedLogFields(
				fields.timestamp(),
				fields.level(),
				fields.message(),
				fields.logger(),
				fields.service(),
				fields.host(),
				fields.traceId(),
				fields.spanId(),
				attrs
		);
	}
}
