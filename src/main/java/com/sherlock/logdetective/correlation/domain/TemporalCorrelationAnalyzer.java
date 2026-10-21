package com.sherlock.logdetective.correlation.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sherlock.logdetective.detection.domain.DetectionFinding;
import com.sherlock.logdetective.detection.domain.DetectionKind;
import com.sherlock.logdetective.detection.domain.DetectionRulesConfig;
import com.sherlock.logdetective.detection.domain.Severity;
import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.EventLevel;

/**
 * Temporal proximity of ERRORs across services. Distinguishes WEAK/STRONG; never claims causality.
 * CONFIRMED is reserved for shared-trace cases (handled by {@link TraceCorrelationAnalyzer}).
 */
public final class TemporalCorrelationAnalyzer {

	private final DetectionRulesConfig.CorrelationConfig config;

	public TemporalCorrelationAnalyzer(DetectionRulesConfig.CorrelationConfig config) {
		this.config = Objects.requireNonNull(config, "config");
	}

	public List<DetectionFinding> analyze(List<CanonicalEvent> errors, Instant now) {
		Objects.requireNonNull(errors, "errors");
		Objects.requireNonNull(now, "now");

		Duration window = config.temporalWindow();
		List<CanonicalEvent> recent = errors.stream()
				.filter(e -> e.level() == EventLevel.ERROR)
				.filter(e -> e.service().isPresent())
				.filter(e -> !e.timestamp().isBefore(now.minus(window.multipliedBy(2))) && !e.timestamp().isAfter(now))
				.sorted(Comparator.comparing(CanonicalEvent::timestamp))
				.toList();

		List<DetectionFinding> findings = new ArrayList<>();
		Set<String> emittedPairs = new LinkedHashSet<>();

		for (int i = 0; i < recent.size(); i++) {
			CanonicalEvent left = recent.get(i);
			for (int j = i + 1; j < recent.size(); j++) {
				CanonicalEvent right = recent.get(j);
				if (left.service().equals(right.service())) {
					continue;
				}
				Duration gap = Duration.between(left.timestamp(), right.timestamp()).abs();
				if (gap.compareTo(window) > 0) {
					break;
				}
				// Skip if already hard-linked by the same trace id.
				if (left.traceId().isPresent() && left.traceId().equals(right.traceId())) {
					continue;
				}
				String pairKey = pairKey(left, right);
				if (!emittedPairs.add(pairKey)) {
					continue;
				}
				CorrelationStrength strength = sharedMessageToken(left, right)
						? CorrelationStrength.STRONG
						: CorrelationStrength.WEAK;
				Set<UUID> eventIds = Set.of(left.id(), right.id());
				String reason = "temporal_correlation: services="
						+ left.service().orElseThrow() + "," + right.service().orElseThrow()
						+ " gapMs=" + gap.toMillis()
						+ " strength=" + strength
						+ " (temporal proximity only; not a causal claim)";
				findings.add(new DetectionFinding(
						DetectionKind.TEMPORAL_CORRELATION,
						Severity.MEDIUM,
						reason,
						"Temporally related errors across services",
						left.service(),
						Optional.empty(),
						strength,
						eventIds,
						Map.of(
								"serviceA", left.service().orElseThrow(),
								"serviceB", right.service().orElseThrow(),
								"gapMs", Long.toString(gap.toMillis()),
								"strength", strength.name()
						),
						now
				));
			}
		}
		return findings;
	}

	private static String pairKey(CanonicalEvent a, CanonicalEvent b) {
		String s1 = a.service().orElse("");
		String s2 = b.service().orElse("");
		if (s1.compareTo(s2) <= 0) {
			return s1 + "|" + s2 + "|" + a.id() + "|" + b.id();
		}
		return s2 + "|" + s1 + "|" + b.id() + "|" + a.id();
	}

	private static boolean sharedMessageToken(CanonicalEvent a, CanonicalEvent b) {
		Set<String> left = tokens(a.message());
		Set<String> right = tokens(b.message());
		left.retainAll(right);
		return left.stream().anyMatch(t -> t.length() >= 5);
	}

	private static Set<String> tokens(String message) {
		return java.util.Arrays.stream(message.toLowerCase(java.util.Locale.ROOT).split("[^a-z0-9]+"))
				.filter(t -> t.length() >= 5)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
