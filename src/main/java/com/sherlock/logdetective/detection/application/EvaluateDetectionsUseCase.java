package com.sherlock.logdetective.detection.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.sherlock.logdetective.correlation.application.CorrelateEvidenceUseCase;
import com.sherlock.logdetective.correlation.domain.Evidence;
import com.sherlock.logdetective.correlation.domain.EvidenceKind;
import com.sherlock.logdetective.correlation.port.out.EvidenceRepository;
import com.sherlock.logdetective.detection.domain.DetectionFinding;
import com.sherlock.logdetective.detection.domain.DetectionRulesConfig;
import com.sherlock.logdetective.detection.domain.ErrorFingerprint;
import com.sherlock.logdetective.detection.domain.ErrorObservation;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.domain.detector.ErrorRateThresholdDetector;
import com.sherlock.logdetective.detection.domain.detector.SpikeDetector;
import com.sherlock.logdetective.detection.domain.fingerprint.ErrorFingerprintCalculator;
import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.EventLevel;
import com.sherlock.logdetective.ingestion.port.out.EventRepository;

/**
 * Runs deterministic detectors and persists resulting incidents + evidence.
 */
public final class EvaluateDetectionsUseCase {

	private final DetectionRulesConfig rulesConfig;
	private final EventRepository eventRepository;
	private final RecordErrorObservationUseCase recordErrorObservationUseCase;
	private final EvidenceRepository evidenceRepository;
	private final CorrelateEvidenceUseCase correlateEvidenceUseCase;
	private final ErrorFingerprintCalculator fingerprintCalculator;

	public EvaluateDetectionsUseCase(
			DetectionRulesConfig rulesConfig,
			EventRepository eventRepository,
			RecordErrorObservationUseCase recordErrorObservationUseCase,
			EvidenceRepository evidenceRepository,
			CorrelateEvidenceUseCase correlateEvidenceUseCase,
			ErrorFingerprintCalculator fingerprintCalculator
	) {
		this.rulesConfig = Objects.requireNonNull(rulesConfig);
		this.eventRepository = Objects.requireNonNull(eventRepository);
		this.recordErrorObservationUseCase = Objects.requireNonNull(recordErrorObservationUseCase);
		this.evidenceRepository = Objects.requireNonNull(evidenceRepository);
		this.correlateEvidenceUseCase = Objects.requireNonNull(correlateEvidenceUseCase);
		this.fingerprintCalculator = Objects.requireNonNull(fingerprintCalculator);
	}

	public EvaluationResult evaluate(Instant now) {
		Objects.requireNonNull(now, "now");
		if (!rulesConfig.enabled()) {
			return EvaluationResult.empty();
		}

		Instant historyFrom = now.minus(rulesConfig.spike().window()
				.multipliedBy(rulesConfig.spike().baselineWindows() + 1L));
		List<CanonicalEvent> history = eventRepository.findBetween(historyFrom, now);
		List<CanonicalEvent> errors = history.stream().filter(e -> e.level() == EventLevel.ERROR).toList();

		List<DetectionFinding> findings = new ArrayList<>();
		findings.addAll(new ErrorRateThresholdDetector(rulesConfig.threshold()).detect(history, now));
		findings.addAll(new SpikeDetector(rulesConfig.spike()).detect(history, now));
		findings.addAll(correlateEvidenceUseCase.correlate(errors, history, now));

		int incidentsTouched = 0;
		int evidenceCreated = 0;
		for (DetectionFinding finding : findings) {
			Incident incident = upsertIncident(finding);
			incidentsTouched++;
			Evidence evidence = toEvidence(finding);
			evidenceRepository.save(evidence);
			evidenceRepository.linkToIncident(incident.id(), evidence);
			evidenceCreated++;
		}
		return new EvaluationResult(findings.size(), incidentsTouched, evidenceCreated, List.copyOf(findings));
	}

	private Incident upsertIncident(DetectionFinding finding) {
		ErrorFingerprint fingerprint = syntheticFingerprint(finding);
		Map<String, String> metadata = new HashMap<>(finding.metrics());
		metadata.put("detectionKind", finding.kind().name());
		metadata.put("detectionReason", finding.reason());
		metadata.put("correlationStrength", finding.strength().name());

		ErrorObservation observation = new ErrorObservation(
				finding.detectedAt(),
				EventLevel.ERROR,
				finding.title() + " | " + finding.reason(),
				Optional.empty(),
				finding.service(),
				Optional.empty(),
				Optional.empty(),
				finding.severity(),
				metadata
		);

		// Bypass message-based fingerprinting: open/update via repository using synthetic fingerprint.
		return recordErrorObservationUseCase.executeWithFingerprint(fingerprint, observation);
	}

	private ErrorFingerprint syntheticFingerprint(DetectionFinding finding) {
		String service = finding.service().orElse("_unknown");
		String signature = switch (finding.kind()) {
			case THRESHOLD_RATE -> "detection|THRESHOLD_RATE|service=" + service
					+ "|window=" + finding.metrics().getOrDefault("windowSeconds", "?");
			case SPIKE -> "detection|SPIKE|service=" + service
					+ "|window=" + finding.metrics().getOrDefault("windowSeconds", "?");
			case TRACE_CORRELATION -> "detection|TRACE|trace="
					+ finding.traceId().orElse("unknown");
			case TEMPORAL_CORRELATION -> "detection|TEMPORAL|"
					+ finding.metrics().getOrDefault("serviceA", "?")
					+ "|" + finding.metrics().getOrDefault("serviceB", "?");
			case ERROR_SEVERITY -> "detection|ERROR_SEVERITY|" + finding.title();
		};
		return fingerprintCalculator.calculate(
				EventLevel.ERROR,
				signature,
				Optional.empty(),
				finding.service()
		);
	}

	private static Evidence toEvidence(DetectionFinding finding) {
		EvidenceKind kind = switch (finding.kind()) {
			case THRESHOLD_RATE, SPIKE, ERROR_SEVERITY -> EvidenceKind.DETECTION_RULE;
			case TRACE_CORRELATION -> EvidenceKind.TRACE_CORRELATION;
			case TEMPORAL_CORRELATION -> EvidenceKind.TEMPORAL_CORRELATION;
		};
		return Evidence.of(
				kind,
				finding.strength(),
				finding.reason(),
				finding.title(),
				finding.contributingEventIds(),
				finding.metrics()
		);
	}

	public record EvaluationResult(
			int findings,
			int incidentsTouched,
			int evidenceCreated,
			List<DetectionFinding> details
	) {
		public static EvaluationResult empty() {
			return new EvaluationResult(0, 0, 0, List.of());
		}
	}
}
