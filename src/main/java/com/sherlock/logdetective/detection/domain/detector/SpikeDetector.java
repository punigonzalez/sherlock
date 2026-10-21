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
 * Simple spike rule: current window error count vs average of previous baseline windows.
 */
public final class SpikeDetector {

	private final DetectionRulesConfig.SpikeConfig config;

	public SpikeDetector(DetectionRulesConfig.SpikeConfig config) {
		this.config = Objects.requireNonNull(config, "config");
	}

	public List<DetectionFinding> detect(List<CanonicalEvent> events, Instant now) {
		Objects.requireNonNull(events, "events");
		Objects.requireNonNull(now, "now");

		long windowSeconds = config.window().toSeconds();
		Instant historyStart = now.minus(config.window().multipliedBy(config.baselineWindows() + 1L));

		Map<String, List<CanonicalEvent>> byService = events.stream()
				.filter(e -> e.level() == EventLevel.ERROR)
				.filter(e -> !e.timestamp().isBefore(historyStart) && !e.timestamp().isAfter(now))
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

			int current = countInWindow(bucket, now.minus(config.window()), now);
			if (current < config.minErrorCount()) {
				continue;
			}

			double baselineSum = 0;
			for (int i = 1; i <= config.baselineWindows(); i++) {
				Instant end = now.minus(config.window().multipliedBy(i));
				Instant start = end.minus(config.window());
				baselineSum += countInWindow(bucket, start, end);
			}
			double baselineAvg = baselineSum / config.baselineWindows();
			double triggerAt = Math.max(config.minErrorCount(), baselineAvg * config.multiplier());
			if (current < triggerAt) {
				continue;
			}

			String service = entry.getKey();
			Optional<String> serviceOpt = "_unknown".equals(service) ? Optional.empty() : Optional.of(service);
			List<CanonicalEvent> currentEvents = bucket.stream()
					.filter(e -> !e.timestamp().isBefore(now.minus(config.window())) && !e.timestamp().isAfter(now))
					.toList();
			String reason = "spike: service=" + service
					+ " current=" + current
					+ " baselineAvg=" + format(baselineAvg)
					+ " multiplier=" + config.multiplier()
					+ " triggerAt=" + format(triggerAt)
					+ " windowSeconds=" + windowSeconds;
			findings.add(new DetectionFinding(
					DetectionKind.SPIKE,
					Severity.CRITICAL,
					reason,
					"Error spike detected for " + service,
					serviceOpt,
					Optional.empty(),
					CorrelationStrength.NONE,
					currentEvents.stream().map(CanonicalEvent::id).collect(Collectors.toSet()),
					Map.of(
							"currentCount", Integer.toString(current),
							"baselineAvg", format(baselineAvg),
							"multiplier", Double.toString(config.multiplier()),
							"triggerAt", format(triggerAt),
							"windowSeconds", Long.toString(windowSeconds)
					),
					now
			));
		}
		return findings;
	}

	private static int countInWindow(List<CanonicalEvent> events, Instant from, Instant to) {
		int count = 0;
		for (CanonicalEvent event : events) {
			if (!event.timestamp().isBefore(from) && !event.timestamp().isAfter(to)) {
				count++;
			}
		}
		return count;
	}

	private static String format(double value) {
		return String.format(java.util.Locale.ROOT, "%.2f", value);
	}
}
