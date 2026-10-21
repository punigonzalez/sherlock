package com.sherlock.logdetective.detection.domain.fingerprint;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sherlock.logdetective.detection.domain.ErrorFingerprint;
import com.sherlock.logdetective.ingestion.domain.EventLevel;

class ErrorFingerprintCalculatorTest {

	private final ErrorFingerprintCalculator calculator = new ErrorFingerprintCalculator();

	@Test
	void sameLogicalErrorYieldsSameFingerprintDespiteVolatileTokens() {
		ErrorFingerprint first = calculator.calculate(
				EventLevel.ERROR,
				"Payment failed for order 998877 at 2026-10-20T22:00:00Z id=550e8400-e29b-41d4-a716-446655440000",
				Optional.of("payments.Worker"),
				Optional.of("payments")
		);
		ErrorFingerprint second = calculator.calculate(
				EventLevel.ERROR,
				"Payment failed for order 112233 at 2026-10-21T01:15:30.123Z id=123e4567-e89b-12d3-a456-426614174000",
				Optional.of("payments.Worker"),
				Optional.of("payments")
		);

		assertThat(first.value()).isEqualTo(second.value());
		assertThat(first.normalizedSignature()).contains("<UUID>").contains("<TS>").contains("<NUM>");
	}

	@Test
	void missingOptionalDimensionsAreOmittedNotInvented() {
		ErrorFingerprint withService = calculator.calculate(
				EventLevel.ERROR,
				"boom",
				Optional.empty(),
				Optional.of("billing")
		);
		ErrorFingerprint withoutService = calculator.calculate(
				EventLevel.ERROR,
				"boom",
				Optional.empty(),
				Optional.empty()
		);

		assertThat(withService.normalizedSignature()).contains("service=billing");
		assertThat(withoutService.normalizedSignature()).doesNotContain("service=");
		assertThat(withService.value()).isNotEqualTo(withoutService.value());
	}
}
