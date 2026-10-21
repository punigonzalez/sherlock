package com.sherlock.logdetective.ingestion.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SafePathValidatorTest {

	@TempDir
	Path tempDir;

	@Test
	void allowsPathsUnderAllowlist() {
		SafePathValidator validator = new SafePathValidator(tempDir);
		Path allowed = tempDir.resolve("app.log");
		assertThat(validator.resolveAllowed(allowed.toString())).isEqualTo(allowed.toAbsolutePath().normalize());
	}

	@Test
	void resolvesRelativePathsAgainstAllowlist() {
		SafePathValidator validator = new SafePathValidator(tempDir);
		Path resolved = validator.resolveAllowed("nested/app.log");
		assertThat(resolved).isEqualTo(tempDir.resolve("nested/app.log").toAbsolutePath().normalize());
	}

	@Test
	void rejectsTraversalOutsideAllowlist() {
		SafePathValidator validator = new SafePathValidator(tempDir);
		assertThatThrownBy(() -> validator.resolveAllowed(tempDir.resolve("..").resolve("escape.log").toString()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("allowlist");
	}

	@Test
	void rejectsBlankPath() {
		SafePathValidator validator = new SafePathValidator(tempDir);
		assertThatThrownBy(() -> validator.resolveAllowed("  "))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
