package com.sherlock.logdetective.detection.domain.detector;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sherlock.logdetective.correlation.domain.CorrelationStrength;
import com.sherlock.logdetective.detection.domain.DetectionFinding;
import com.sherlock.logdetective.detection.domain.DetectionKind;
import com.sherlock.logdetective.detection.domain.DetectionRulesConfig;
import com.sherlock.logdetective.detection.domain.Severity;
import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.EventLevel;

/**
 * Fires when ERROR count in a sliding window reaches a configured threshold (per service key).
 */
public final class ErrorRateThresholdDetector {

	private final DetectionRulesConfig.ThresholdConfig config;

	public ErrorRateThresholdDetector(DetectionRulesConfig.ThresholdConfig config) {
		this.config = Objects.requireNonNull(config, "config");
	}

	public List<DetectionFinding> detect(List<CanonicalEvent> events, Instant now) {
		Objects.requireNonNull(events, "events");
		Objects.requireNonNull(now, "now");
		Instant from = now.minus(config.window());

		Map<String, List<CanonicalEvent>> byService = events.stream()
				.filter(e -> e.level() == EventLevel.ERROR)
				.filter(e -> !e.timestamp().isBefore(from) && !e.timestamp().isAfter(now))
				.collect(Collectors.groupingBy(
						e -> e.service().orElse("_unknown"),
						LinkedHashMap::new,
						Collectors.toList()
				));

		List<DetectionFinding> findings = new ArrayList<>();
		for (Map.Entry<String, List<CanonicalEvent>> entry : byService.entrySet()) {
			List<CanonicalEvent> bucket = entry.getValue().stream()
					.sorted(Comparator.comparing(CanonicalEvent::timestamp))
					.toList();
			if (bucket.size() < config.minErrorCount()) {
				continue;
			}
			String service = entry.getKey();
			Optional<String> serviceOpt = "_unknown".equals(service) ? Optional.empty() : Optional.of(service);
			String reason = "threshold_rate: service=" + service
					+ " errors=" + bucket.size()
					+ " >= min=" + config.minErrorCount()
					+ " windowSeconds=" + config.window().toSeconds();
			findings.add(new DetectionFinding(
					DetectionKind.THRESHOLD_RATE,
					Severity.HIGH,
					reason,
					"Error rate threshold exceeded for " + service,
					serviceOpt,
					Optional.empty(),
					CorrelationStrength.NONE,
					bucket.stream().map(CanonicalEvent::id).collect(Collectors.toSet()),
					Map.of(
							"errorCount", Integer.toString(bucket.size()),
							"minErrorCount", Integer.toString(config.minErrorCount()),
							"windowSeconds", Long.toString(config.window().toSeconds())
					),
					now
			));
		}
		return findings;
	}
}
