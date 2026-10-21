package com.sherlock.logdetective.detection.domain.detector;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sherlock.logdetective.detection.domain.DetectionKind;
import com.sherlock.logdetective.detection.domain.DetectionRulesConfig;
import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.EventLevel;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;

class SpikeDetectorTest {

	@Test
	void detectsSuddenIncreaseAgainstBaseline() {
		SpikeDetector detector = new SpikeDetector(
				new DetectionRulesConfig.SpikeConfig(Duration.ofSeconds(60), 2, 3.0d, 3)
		);
		Instant now = Instant.parse("2026-10-21T12:00:00Z");
		List<CanonicalEvent> events = new ArrayList<>();
		// baseline windows: 1 error each
		events.add(event(now.minusSeconds(150), "api"));
		events.add(event(now.minusSeconds(90), "api"));
		// current window: 6 errors
		for (int i = 0; i < 6; i++) {
			events.add(event(now.minusSeconds(i), "api"));
		}

		var findings = detector.detect(events, now);
		assertThat(findings).hasSize(1);
		assertThat(findings.getFirst().kind()).isEqualTo(DetectionKind.SPIKE);
		assertThat(findings.getFirst().reason()).contains("spike:");
	}

	private static CanonicalEvent event(Instant ts, String service) {
		return new CanonicalEvent(
				UUID.randomUUID(),
				new LogSourceId("src"),
				UUID.randomUUID().toString(),
				ts,
				ts,
				EventLevel.ERROR,
				"err",
				Optional.empty(),
				Optional.of(service),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Map.of()
		);
	}
}
