package com.sherlock.logdetective.investigation.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.sherlock.logdetective.correlation.domain.EvidenceId;

/**
 * Evidence-grounded diagnostic report. Citations must reference real evidence ids when present.
 */
public record InvestigationReport(
		UUID id,
		UUID incidentId,
		InvestigationStatus status,
		String provider,
		String summary,
		String hypothesis,
		Confidence confidence,
		List<EvidenceId> evidenceCitations,
		List<ToolCallRecord> toolCalls,
		Map<String, String> metadata,
		Instant createdAt
) {

	public InvestigationReport {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(incidentId, "incidentId");
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(provider, "provider");
		Objects.requireNonNull(summary, "summary");
		hypothesis = hypothesis == null ? "" : hypothesis;
		Objects.requireNonNull(confidence, "confidence");
		evidenceCitations = evidenceCitations == null ? List.of() : List.copyOf(evidenceCitations);
		toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
		Objects.requireNonNull(createdAt, "createdAt");
	}

	public static InvestigationReport noop(UUID incidentId, List<EvidenceId> evidenceIds) {
		return new InvestigationReport(
				UUID.randomUUID(),
				incidentId,
				InvestigationStatus.NOOP_UNAVAILABLE,
				"noop",
				"AI investigation disabled (NoOp). Deterministic detection remains available.",
				"",
				Confidence.NONE,
				evidenceIds,
				List.of(),
				Map.of("aiUnavailable", "true"),
				Instant.now()
		);
	}

	public enum InvestigationStatus {
		NOOP_UNAVAILABLE,
		COMPLETED,
		FAILED,
		UNSUPPORTED
	}

	public enum Confidence {
		NONE,
		LOW,
		MEDIUM,
		HIGH
	}

	public record ToolCallRecord(
			String tool,
			String outcome,
			String detail
	) {
		public ToolCallRecord {
			Objects.requireNonNull(tool, "tool");
			Objects.requireNonNull(outcome, "outcome");
			detail = detail == null ? "" : detail;
		}
	}
}
