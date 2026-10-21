package com.sherlock.logdetective.detection.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * Pure config values for deterministic detectors (no Spring types).
 */
public record DetectionRulesConfig(
		boolean enabled,
		ThresholdConfig threshold,
		SpikeConfig spike,
		CorrelationConfig correlation
) {

	public DetectionRulesConfig {
		Objects.requireNonNull(threshold, "threshold");
		Objects.requireNonNull(spike, "spike");
		Objects.requireNonNull(correlation, "correlation");
	}

	public static DetectionRulesConfig defaults() {
		return new DetectionRulesConfig(
				true,
				new ThresholdConfig(Duration.ofSeconds(60), 5),
				new SpikeConfig(Duration.ofSeconds(60), 3, 3.0d, 3),
				new CorrelationConfig(Duration.ofSeconds(300), Duration.ofSeconds(30))
		);
	}

	public record ThresholdConfig(Duration window, int minErrorCount) {
		public ThresholdConfig {
			Objects.requireNonNull(window, "window");
			if (window.isNegative() || window.isZero()) {
				throw new IllegalArgumentException("window must be positive");
			}
			if (minErrorCount < 1) {
				throw new IllegalArgumentException("minErrorCount must be >= 1");
			}
		}
	}

	public record SpikeConfig(
			Duration window,
			int baselineWindows,
			double multiplier,
			int minErrorCount
	) {
		public SpikeConfig {
			Objects.requireNonNull(window, "window");
			if (window.isNegative() || window.isZero()) {
				throw new IllegalArgumentException("window must be positive");
			}
			if (baselineWindows < 1) {
				throw new IllegalArgumentException("baselineWindows must be >= 1");
			}
			if (multiplier < 1.0d) {
				throw new IllegalArgumentException("multiplier must be >= 1.0");
			}
			if (minErrorCount < 1) {
				throw new IllegalArgumentException("minErrorCount must be >= 1");
			}
		}
	}

	public record CorrelationConfig(Duration traceLookback, Duration temporalWindow) {
		public CorrelationConfig {
			Objects.requireNonNull(traceLookback, "traceLookback");
			Objects.requireNonNull(temporalWindow, "temporalWindow");
			if (traceLookback.isNegative() || traceLookback.isZero()) {
				throw new IllegalArgumentException("traceLookback must be positive");
			}
			if (temporalWindow.isNegative() || temporalWindow.isZero()) {
				throw new IllegalArgumentException("temporalWindow must be positive");
			}
		}
	}
}
