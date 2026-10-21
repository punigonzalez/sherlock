package com.sherlock.logdetective.ingestion.domain;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Prevents path traversal outside an allowlisted base directory.
 */
public final class SafePathValidator {

	private final Path allowlistBase;

	public SafePathValidator(Path allowlistBase) {
		this.allowlistBase = Objects.requireNonNull(allowlistBase, "allowlistBase")
				.toAbsolutePath()
				.normalize();
	}

	public Path allowlistBase() {
		return allowlistBase;
	}

	public Path resolveAllowed(String userPath) {
		Objects.requireNonNull(userPath, "userPath");
		if (userPath.isBlank()) {
			throw new IllegalArgumentException("path must not be blank");
		}
		Path asGiven = Path.of(userPath);
		Path resolved = asGiven.toAbsolutePath().normalize();
		if (resolved.startsWith(allowlistBase)) {
			return resolved;
		}
		// Treat relative paths as rooted at the allowlist base (e.g. "app.log").
		if (!asGiven.isAbsolute()) {
			Path underBase = allowlistBase.resolve(asGiven).normalize().toAbsolutePath();
			if (underBase.startsWith(allowlistBase)) {
				return underBase;
			}
		}
		throw new IllegalArgumentException(
				"path escapes allowlist base directory: " + allowlistBase
		);
	}
}
