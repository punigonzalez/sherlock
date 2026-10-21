package com.sherlock.logdetective.detection.domain.detector;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.sherlock.logdetective.detection.domain.DetectionKind;
import com.sherlock.logdetective.detection.domain.DetectionRulesConfig;
import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.EventLevel;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;

class ErrorRateThresholdDetectorTest {

	@Test
	void firesWhenWindowCountMeetsThreshold() {
		ErrorRateThresholdDetector detector = new ErrorRateThresholdDetector(
				new DetectionRulesConfig.ThresholdConfig(Duration.ofSeconds(60), 3)
		);
		Instant now = Instant.parse("2026-10-21T12:00:00Z");
		List<CanonicalEvent> events = IntStream.range(0, 3)
				.mapToObj(i -> event("billing", now.minusSeconds(10L - i), EventLevel.ERROR))
				.toList();

		assertThat(detector.detect(events, now)).hasSize(1);
		assertThat(detector.detect(events, now).getFirst().kind()).isEqualTo(DetectionKind.THRESHOLD_RATE);
		assertThat(detector.detect(events, now).getFirst().reason()).contains("errors=3");
	}

	@Test
	void doesNotFireBelowThreshold() {
		ErrorRateThresholdDetector detector = new ErrorRateThresholdDetector(
				new DetectionRulesConfig.ThresholdConfig(Duration.ofSeconds(60), 5)
		);
		Instant now = Instant.parse("2026-10-21T12:00:00Z");
		List<CanonicalEvent> events = List.of(
				event("billing", now.minusSeconds(5), EventLevel.ERROR),
				event("billing", now.minusSeconds(4), EventLevel.ERROR)
		);
		assertThat(detector.detect(events, now)).isEmpty();
	}

	private static CanonicalEvent event(String service, Instant ts, EventLevel level) {
		return new CanonicalEvent(
				UUID.randomUUID(),
				new LogSourceId("src"),
				UUID.randomUUID().toString(),
				ts,
				ts,
				level,
				"boom",
				Optional.empty(),
				Optional.of(service),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Map.of()
		);
	}
}
