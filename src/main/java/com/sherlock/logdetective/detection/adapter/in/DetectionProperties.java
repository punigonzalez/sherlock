package com.sherlock.logdetective.detection.adapter.in;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.sherlock.logdetective.detection.domain.DetectionRulesConfig;

@ConfigurationProperties(prefix = "logdetective.detection")
public class DetectionProperties {

	private boolean enabled = true;
	private Threshold threshold = new Threshold();
	private Spike spike = new Spike();
	private Correlation correlation = new Correlation();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Threshold getThreshold() {
		return threshold;
	}

	public void setThreshold(Threshold threshold) {
		this.threshold = threshold;
	}

	public Spike getSpike() {
		return spike;
	}

	public void setSpike(Spike spike) {
		this.spike = spike;
	}

	public Correlation getCorrelation() {
		return correlation;
	}

	public void setCorrelation(Correlation correlation) {
		this.correlation = correlation;
	}

	public DetectionRulesConfig toRulesConfig() {
		return new DetectionRulesConfig(
				enabled,
				new DetectionRulesConfig.ThresholdConfig(
						Duration.ofSeconds(threshold.windowSeconds),
						threshold.minErrorCount
				),
				new DetectionRulesConfig.SpikeConfig(
						Duration.ofSeconds(spike.windowSeconds),
						spike.baselineWindows,
						spike.multiplier,
						spike.minErrorCount
				),
				new DetectionRulesConfig.CorrelationConfig(
						Duration.ofSeconds(correlation.traceLookbackSeconds),
						Duration.ofSeconds(correlation.temporalWindowSeconds)
				)
		);
	}

	public static class Threshold {
		private long windowSeconds = 60;
		private int minErrorCount = 5;

		public long getWindowSeconds() {
			return windowSeconds;
		}

		public void setWindowSeconds(long windowSeconds) {
			this.windowSeconds = windowSeconds;
		}

		public int getMinErrorCount() {
			return minErrorCount;
		}

		public void setMinErrorCount(int minErrorCount) {
			this.minErrorCount = minErrorCount;
		}
	}

	public static class Spike {
		private long windowSeconds = 60;
		private int baselineWindows = 3;
		private double multiplier = 3.0d;
		private int minErrorCount = 3;

		public long getWindowSeconds() {
			return windowSeconds;
		}

		public void setWindowSeconds(long windowSeconds) {
			this.windowSeconds = windowSeconds;
		}

		public int getBaselineWindows() {
			return baselineWindows;
		}

		public void setBaselineWindows(int baselineWindows) {
			this.baselineWindows = baselineWindows;
		}

		public double getMultiplier() {
			return multiplier;
		}

		public void setMultiplier(double multiplier) {
			this.multiplier = multiplier;
		}

		public int getMinErrorCount() {
			return minErrorCount;
		}

		public void setMinErrorCount(int minErrorCount) {
			this.minErrorCount = minErrorCount;
		}
	}

	public static class Correlation {
		private long traceLookbackSeconds = 300;
		private long temporalWindowSeconds = 30;

		public long getTraceLookbackSeconds() {
			return traceLookbackSeconds;
		}

		public void setTraceLookbackSeconds(long traceLookbackSeconds) {
			this.traceLookbackSeconds = traceLookbackSeconds;
		}

		public long getTemporalWindowSeconds() {
			return temporalWindowSeconds;
		}

		public void setTemporalWindowSeconds(long temporalWindowSeconds) {
			this.temporalWindowSeconds = temporalWindowSeconds;
		}
	}
}
