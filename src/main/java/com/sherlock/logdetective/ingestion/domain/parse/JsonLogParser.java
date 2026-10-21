package com.sherlock.logdetective.ingestion.domain.parse;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.sherlock.logdetective.ingestion.domain.EventLevel;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class JsonLogParser implements LogParser {

	private final JsonMapper jsonMapper;

	public JsonLogParser() {
		this(JsonMapper.shared());
	}

	public JsonLogParser(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public ParseResult parse(String rawBody, Instant fallbackTimestamp) {
		String trimmed = rawBody == null ? "" : rawBody.trim();
		if (!trimmed.startsWith("{")) {
			return ParseResult.failure("not-json-object");
		}
		try {
			JsonNode root = jsonMapper.readTree(trimmed);
			if (root == null || !root.isObject()) {
				return ParseResult.failure("json-root-not-object");
			}
			String message = text(root, "message", "msg", "log");
			if (message == null || message.isBlank()) {
				return ParseResult.failure("json-missing-message");
			}
			EventLevel level = parseLevel(text(root, "level", "severity", "log.level"));
			Instant timestamp = parseTimestamp(text(root, "timestamp", "@timestamp", "time", "ts"), fallbackTimestamp);
			Map<String, String> attributes = new HashMap<>();
			Iterator<Map.Entry<String, JsonNode>> fields = root.properties().iterator();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = fields.next();
				if (entry.getValue().isValueNode()) {
					attributes.put(entry.getKey(), entry.getValue().asString());
				}
			}
			return ParseResult.ok(new ParsedLogFields(
					timestamp,
					level,
					message,
					optional(text(root, "logger", "loggerName")),
					optional(text(root, "service", "service.name", "app")),
					optional(text(root, "host", "hostname")),
					optional(text(root, "traceId", "trace_id", "dd.trace_id")),
					optional(text(root, "spanId", "span_id", "dd.span_id")),
					attributes
			));
		}
		catch (Exception ex) {
			return ParseResult.failure("json-parse-error:" + ex.getClass().getSimpleName());
		}
	}

	private static String text(JsonNode root, String... keys) {
		for (String key : keys) {
			JsonNode node = root.get(key);
			if (node != null && !node.isNull()) {
				return node.asString();
			}
		}
		return null;
	}

	private static Optional<String> optional(String value) {
		return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
	}

	private static EventLevel parseLevel(String raw) {
		if (raw == null || raw.isBlank()) {
			return EventLevel.UNKNOWN;
		}
		return switch (raw.trim().toUpperCase()) {
			case "TRACE" -> EventLevel.TRACE;
			case "DEBUG" -> EventLevel.DEBUG;
			case "INFO", "INFORMATION" -> EventLevel.INFO;
			case "WARN", "WARNING" -> EventLevel.WARN;
			case "ERROR", "ERR", "FATAL", "SEVERE" -> EventLevel.ERROR;
			default -> EventLevel.UNKNOWN;
		};
	}

	private static Instant parseTimestamp(String raw, Instant fallback) {
		if (raw == null || raw.isBlank()) {
			return fallback;
		}
		try {
			return Instant.parse(raw);
		}
		catch (DateTimeParseException ignored) {
			return fallback;
		}
	}
}
