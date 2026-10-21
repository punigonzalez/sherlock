package com.sherlock.logdetective.ingestion.domain.parse;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sherlock.logdetective.ingestion.domain.EventLevel;

/**
 * Conventional text logs, e.g.
 * {@code 2026-10-20 12:00:00,123 ERROR [main] com.example.App - boom}
 */
public final class TextLogParser implements LogParser {

	private static final Pattern PATTERN = Pattern.compile(
			"^(?<ts>\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?)\\s+"
					+ "(?<level>TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE)\\s+"
					+ "(?:\\[(?<thread>[^\\]]+)]\\s+)?"
					+ "(?<logger>[\\w.$]+)?\\s*-?\\s*(?<message>.*)$",
			Pattern.DOTALL
	);

	private static final DateTimeFormatter TS = new DateTimeFormatterBuilder()
			.appendPattern("yyyy-MM-dd HH:mm:ss")
			.optionalStart()
			.appendLiteral(',')
			.appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false)
			.optionalEnd()
			.optionalStart()
			.appendLiteral('.')
			.appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false)
			.optionalEnd()
			.toFormatter(Locale.ROOT);

	@Override
	public ParseResult parse(String rawBody, Instant fallbackTimestamp) {
		if (rawBody == null || rawBody.isBlank()) {
			return ParseResult.failure("empty-text");
		}
		Matcher matcher = PATTERN.matcher(rawBody.trim());
		if (!matcher.matches()) {
			return ParseResult.ok(new ParsedLogFields(
					fallbackTimestamp,
					EventLevel.UNKNOWN,
					rawBody,
					Optional.empty(),
					Optional.empty(),
					Optional.empty(),
					Optional.empty(),
					Optional.empty(),
					Map.of("format", "free-text")
			));
		}
		Instant timestamp = parseTs(matcher.group("ts"), fallbackTimestamp);
		EventLevel level = parseLevel(matcher.group("level"));
		String logger = matcher.group("logger");
		String message = matcher.group("message");
		if (message == null || message.isBlank()) {
			message = rawBody;
		}
		return ParseResult.ok(new ParsedLogFields(
				timestamp,
				level,
				message,
				optional(logger),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Map.of("format", "conventional-text")
		));
	}

	private static Instant parseTs(String raw, Instant fallback) {
		String normalized = raw.replace('T', ' ');
		try {
			LocalDateTime ldt = LocalDateTime.parse(normalized, TS);
			return ldt.toInstant(ZoneOffset.UTC);
		}
		catch (DateTimeParseException ex) {
			return fallback;
		}
	}

	private static EventLevel parseLevel(String raw) {
		return switch (raw.toUpperCase(Locale.ROOT)) {
			case "TRACE" -> EventLevel.TRACE;
			case "DEBUG" -> EventLevel.DEBUG;
			case "INFO" -> EventLevel.INFO;
			case "WARN", "WARNING" -> EventLevel.WARN;
			case "ERROR", "FATAL", "SEVERE" -> EventLevel.ERROR;
			default -> EventLevel.UNKNOWN;
		};
	}

	private static Optional<String> optional(String value) {
		return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
	}
}
