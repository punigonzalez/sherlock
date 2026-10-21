package com.sherlock.logdetective.correlation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
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

class TemporalCorrelationAnalyzerTest {

	@Test
	void weakWhenOnlyTemporalProximity() {
		TemporalCorrelationAnalyzer analyzer = new TemporalCorrelationAnalyzer(
				new DetectionRulesConfig.CorrelationConfig(Duration.ofMinutes(5), Duration.ofSeconds(30))
		);
		Instant now = Instant.parse("2026-10-21T12:00:00Z");
		CanonicalEvent a = event(now.minusSeconds(5), "checkout", "alpha failure mode");
		CanonicalEvent b = event(now.minusSeconds(2), "inventory", "beta unrelated zz");
		var findings = analyzer.analyze(List.of(a, b), now);
		assertThat(findings).hasSize(1);
		assertThat(findings.getFirst().kind()).isEqualTo(DetectionKind.TEMPORAL_CORRELATION);
		assertThat(findings.getFirst().strength()).isEqualTo(CorrelationStrength.WEAK);
		assertThat(findings.getFirst().reason()).contains("not a causal claim");
	}

	@Test
	void strongWhenSharedMessageToken() {
		TemporalCorrelationAnalyzer analyzer = new TemporalCorrelationAnalyzer(
				new DetectionRulesConfig.CorrelationConfig(Duration.ofMinutes(5), Duration.ofSeconds(30))
		);
		Instant now = Instant.parse("2026-10-21T12:00:00Z");
		CanonicalEvent a = event(now.minusSeconds(5), "checkout", "payment timeout upstream");
		CanonicalEvent b = event(now.minusSeconds(2), "payments", "payment timeout downstream");
		var findings = analyzer.analyze(List.of(a, b), now);
		assertThat(findings).hasSize(1);
		assertThat(findings.getFirst().strength()).isEqualTo(CorrelationStrength.STRONG);
	}

	private static CanonicalEvent event(Instant ts, String service, String message) {
		return new CanonicalEvent(
				UUID.randomUUID(),
				new LogSourceId("src"),
				UUID.randomUUID().toString(),
				ts,
				ts,
				EventLevel.ERROR,
				message,
				Optional.empty(),
				Optional.of(service),
				Optional.empty(),
				Optional.empty(),
				Optional.empty(),
				Map.of()
		);
	}
}
