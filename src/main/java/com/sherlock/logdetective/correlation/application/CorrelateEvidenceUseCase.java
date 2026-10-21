package com.sherlock.logdetective.correlation.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.sherlock.logdetective.correlation.domain.TemporalCorrelationAnalyzer;
import com.sherlock.logdetective.correlation.domain.TraceCorrelationAnalyzer;
import com.sherlock.logdetective.detection.domain.DetectionFinding;
import com.sherlock.logdetective.detection.domain.DetectionRulesConfig;
import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;

/**
 * Runs trace-ID and temporal correlation analyzers. Does not invent causal claims.
 */
public final class CorrelateEvidenceUseCase {

	private final TraceCorrelationAnalyzer traceAnalyzer;
	private final TemporalCorrelationAnalyzer temporalAnalyzer;

	public CorrelateEvidenceUseCase(DetectionRulesConfig.CorrelationConfig config) {
		Objects.requireNonNull(config, "config");
		this.traceAnalyzer = new TraceCorrelationAnalyzer(config);
		this.temporalAnalyzer = new TemporalCorrelationAnalyzer(config);
	}

	public List<DetectionFinding> correlate(
			List<CanonicalEvent> seedErrors,
			List<CanonicalEvent> candidateEvents,
			Instant now
	) {
		Objects.requireNonNull(seedErrors, "seedErrors");
		Objects.requireNonNull(candidateEvents, "candidateEvents");
		Objects.requireNonNull(now, "now");

		List<DetectionFinding> findings = new ArrayList<>();
		findings.addAll(traceAnalyzer.analyze(seedErrors, candidateEvents, now));
		findings.addAll(temporalAnalyzer.analyze(seedErrors, now));
		return List.copyOf(findings);
	}
}
