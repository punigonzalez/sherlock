package com.sherlock.logdetective.correlation.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.sherlock.logdetective.detection.domain.DetectionFinding;
import com.sherlock.logdetective.detection.domain.DetectionKind;
import com.sherlock.logdetective.detection.domain.DetectionRulesConfig;
import com.sherlock.logdetective.detection.domain.Severity;
import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.EventLevel;

/**
 * Correlates events that share a trace ID. Strength reflects breadth of evidence, not causality.
 */
public final class TraceCorrelationAnalyzer {

	private final DetectionRulesConfig.CorrelationConfig config;

	public TraceCorrelationAnalyzer(DetectionRulesConfig.CorrelationConfig config) {
		this.config = Objects.requireNonNull(config, "config");
	}

	public List<DetectionFinding> analyze(List<CanonicalEvent> seedErrors, List<CanonicalEvent> candidates, Instant now) {
		Objects.requireNonNull(seedErrors, "seedErrors");
		Objects.requireNonNull(candidates, "candidates");
		Objects.requireNonNull(now, "now");

		Instant from = now.minus(config.traceLookback());
		Map<String, List<CanonicalEvent>> byTrace = candidates.stream()
				.filter(e -> e.traceId().isPresent())
				.filter(e -> !e.timestamp().isBefore(from) && !e.timestamp().isAfter(now))
				.collect(Collectors.groupingBy(e -> e.traceId().orElseThrow()));

		Set<String> seen = new LinkedHashSet<>();
		List<DetectionFinding> findings = new ArrayList<>();
		for (CanonicalEvent seed : seedErrors) {
			if (seed.level() != EventLevel.ERROR || seed.traceId().isEmpty()) {
				continue;
			}
			String traceId = seed.traceId().orElseThrow();
			if (!seen.add(traceId)) {
				continue;
			}
			List<CanonicalEvent> related = byTrace.getOrDefault(traceId, List.of());
			if (related.size() < 2) {
				continue;
			}
			Set<String> services = related.stream()
					.map(e -> e.service().orElse("_unknown"))
					.collect(Collectors.toCollection(LinkedHashSet::new));
			CorrelationStrength strength = services.size() >= 2
					? CorrelationStrength.CONFIRMED
					: (related.size() >= 3 ? CorrelationStrength.STRONG : CorrelationStrength.WEAK);
			String reason = "trace_correlation: traceId=" + traceId
					+ " events=" + related.size()
					+ " services=" + services.size()
					+ " strength=" + strength
					+ " (shared trace id; not a causal claim)";
			findings.add(new DetectionFinding(
					DetectionKind.TRACE_CORRELATION,
					Severity.HIGH,
					reason,
					"Trace-correlated events for " + traceId,
					seed.service(),
					Optional.of(traceId),
					strength,
					related.stream().map(CanonicalEvent::id).collect(Collectors.toSet()),
					Map.of(
							"traceId", traceId,
							"eventCount", Integer.toString(related.size()),
							"serviceCount", Integer.toString(services.size()),
							"strength", strength.name()
					),
					now
			));
		}
		return findings;
	}
}
