package com.sherlock.logdetective.detection.domain.fingerprint;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Replaces volatile tokens (UUIDs, timestamps, hex ids, long numbers) with stable placeholders
 * so fingerprints group semantically identical errors.
 */
public final class VolatileTokenNormalizer {

	private static final Pattern UUID = Pattern.compile(
			"\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b"
	);
	private static final Pattern ISO_INSTANT = Pattern.compile(
			"\\b\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?\\b"
	);
	private static final Pattern HEX_ID = Pattern.compile("\\b[0-9a-fA-F]{16,}\\b");
	private static final Pattern LONG_NUMBER = Pattern.compile("\\b\\d{6,}\\b");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	public String normalize(String input) {
		Objects.requireNonNull(input, "input");
		String value = input.trim();
		value = UUID.matcher(value).replaceAll("<UUID>");
		value = ISO_INSTANT.matcher(value).replaceAll("<TS>");
		value = HEX_ID.matcher(value).replaceAll("<HEX>");
		value = LONG_NUMBER.matcher(value).replaceAll("<NUM>");
		value = WHITESPACE.matcher(value).replaceAll(" ");
		return value.trim();
	}
}
