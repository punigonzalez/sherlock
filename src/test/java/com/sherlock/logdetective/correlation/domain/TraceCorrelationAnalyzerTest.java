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

class TraceCorrelationAnalyzerTest {

	@Test
	void confirmedWhenMultipleServicesShareTrace() {
		TraceCorrelationAnalyzer analyzer = new TraceCorrelationAnalyzer(
				new DetectionRulesConfig.CorrelationConfig(Duration.ofMinutes(5), Duration.ofSeconds(30))
		);
		Instant now = Instant.parse("2026-10-21T12:00:00Z");
		CanonicalEvent a = event(now.minusSeconds(2), "checkout", "trace-1", EventLevel.ERROR);
		CanonicalEvent b = event(now.minusSeconds(1), "payments", "trace-1", EventLevel.INFO);
		var findings = analyzer.analyze(List.of(a), List.of(a, b), now);
		assertThat(findings).hasSize(1);
		assertThat(findings.getFirst().kind()).isEqualTo(DetectionKind.TRACE_CORRELATION);
		assertThat(findings.getFirst().strength()).isEqualTo(CorrelationStrength.CONFIRMED);
		assertThat(findings.getFirst().reason()).contains("not a causal claim");
	}

	private static CanonicalEvent event(Instant ts, String service, String trace, EventLevel level) {
		return new CanonicalEvent(
				UUID.randomUUID(),
				new LogSourceId("src"),
				UUID.randomUUID().toString(),
				ts,
				ts,
				level,
				"msg",
				Optional.empty(),
				Optional.of(service),
				Optional.empty(),
				Optional.of(trace),
				Optional.empty(),
				Map.of()
		);
	}
}
