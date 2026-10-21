package com.sherlock.logdetective.detection.domain;

import java.util.Objects;

/**
 * Deterministic fingerprint of an error signature after volatile-token normalization.
 */
public record ErrorFingerprint(String value, String normalizedSignature) {

	public ErrorFingerprint {
		Objects.requireNonNull(value, "value");
		Objects.requireNonNull(normalizedSignature, "normalizedSignature");
		if (value.isBlank()) {
			throw new IllegalArgumentException("fingerprint value must not be blank");
		}
		if (normalizedSignature.isBlank()) {
			throw new IllegalArgumentException("normalizedSignature must not be blank");
		}
	}
}
