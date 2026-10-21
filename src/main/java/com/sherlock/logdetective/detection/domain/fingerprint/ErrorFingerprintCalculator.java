package com.sherlock.logdetective.detection.domain.fingerprint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.sherlock.logdetective.detection.domain.ErrorFingerprint;
import com.sherlock.logdetective.ingestion.domain.EventLevel;

/**
 * Builds deterministic fingerprints from stable error dimensions only.
 * Missing optional dimensions are omitted (never invented).
 */
public final class ErrorFingerprintCalculator {

	private final VolatileTokenNormalizer normalizer;

	public ErrorFingerprintCalculator() {
		this(new VolatileTokenNormalizer());
	}

	public ErrorFingerprintCalculator(VolatileTokenNormalizer normalizer) {
		this.normalizer = Objects.requireNonNull(normalizer, "normalizer");
	}

	public ErrorFingerprint calculate(
			EventLevel level,
			String message,
			Optional<String> logger,
			Optional<String> service
	) {
		Objects.requireNonNull(level, "level");
		Objects.requireNonNull(message, "message");
		Objects.requireNonNull(logger, "logger");
		Objects.requireNonNull(service, "service");

		String normalizedMessage = normalizer.normalize(message);
		if (normalizedMessage.isBlank()) {
			throw new IllegalArgumentException("message must not be blank after normalization");
		}

		StringBuilder signature = new StringBuilder();
		signature.append("level=").append(level.name());
		signature.append("|msg=").append(normalizedMessage);
		logger.map(normalizer::normalize).filter(s -> !s.isBlank()).ifPresent(v ->
				signature.append("|logger=").append(v.toLowerCase(Locale.ROOT))
		);
		service.map(normalizer::normalize).filter(s -> !s.isBlank()).ifPresent(v ->
				signature.append("|service=").append(v.toLowerCase(Locale.ROOT))
		);

		String normalizedSignature = signature.toString();
		return new ErrorFingerprint(sha256Hex(normalizedSignature), normalizedSignature);
	}

	private static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}
}
