package com.sherlock.logdetective.ingestion.domain.redaction;

import java.util.regex.Pattern;

/**
 * Masks common secrets/PII tokens without inventing replacement semantics beyond placeholders.
 */
public final class BasicRedactionHook implements RedactionHook {

	private static final Pattern EMAIL = Pattern.compile(
			"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
	);
	private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[a-z0-9._\\-]+");
	private static final Pattern API_KEY = Pattern.compile("(?i)(api[_-]?key\\s*[=:]\\s*)\\S+");

	@Override
	public String redact(String message) {
		if (message == null || message.isBlank()) {
			return message;
		}
		String value = EMAIL.matcher(message).replaceAll("<REDACTED_EMAIL>");
		value = BEARER.matcher(value).replaceAll("$1<REDACTED_TOKEN>");
		value = API_KEY.matcher(value).replaceAll("$1<REDACTED_SECRET>");
		return value;
	}
}
