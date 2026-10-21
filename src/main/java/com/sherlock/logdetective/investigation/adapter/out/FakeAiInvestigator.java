package com.sherlock.logdetective.investigation.adapter.out;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.sherlock.logdetective.correlation.domain.EvidenceId;
import com.sherlock.logdetective.investigation.domain.InvestigationReport;
import com.sherlock.logdetective.investigation.domain.InvestigationReport.Confidence;
import com.sherlock.logdetective.investigation.domain.InvestigationReport.InvestigationStatus;
import com.sherlock.logdetective.investigation.domain.InvestigationReport.ToolCallRecord;
import com.sherlock.logdetective.investigation.port.out.AiInvestigator;
import com.sherlock.logdetective.investigation.port.out.InvestigationTools;
import com.sherlock.logdetective.investigation.port.out.InvestigationTools.ToolResult;

/**
 * Deterministic stub investigator for tests/E2E. Uses bounded tools; never calls a real LLM.
 */
public final class FakeAiInvestigator implements AiInvestigator {

	private final InvestigationTools tools;
	private final int maxToolCalls;

	public FakeAiInvestigator(InvestigationTools tools, int maxToolCalls) {
		this.tools = Objects.requireNonNull(tools);
		this.maxToolCalls = Math.max(1, maxToolCalls);
	}

	@Override
	public InvestigationReport investigate(UUID incidentId, List<EvidenceId> evidenceIds) {
		List<ToolCallRecord> calls = new ArrayList<>();
		List<EvidenceId> citations = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);

		ToolResult details = record(calls, tools.getIncidentDetails(incidentId));
		if (calls.size() < maxToolCalls) {
			record(calls, tools.getRelatedErrors(incidentId, 10));
		}
		if (calls.size() < maxToolCalls) {
			record(calls, tools.getRecentEvents(5));
		}

		String title = details.rows().isEmpty() ? "unknown" : details.rows().getFirst().getOrDefault("title", "unknown");
		String hypothesis = "Recurring error pattern around: " + stripTags(title)
				+ ". Correlate with linked evidence; not a root-cause claim.";
		return new InvestigationReport(
				UUID.randomUUID(),
				incidentId,
				InvestigationStatus.COMPLETED,
				"fake",
				"Fake investigator completed a bounded tool walk (no external LLM).",
				hypothesis,
				citations.isEmpty() ? Confidence.LOW : Confidence.MEDIUM,
				citations,
				calls,
				Map.of("aiUnavailable", "false", "mode", "fake"),
				Instant.now()
		);
	}

	private static ToolResult record(List<ToolCallRecord> calls, ToolResult result) {
		calls.add(new ToolCallRecord(result.tool(), result.outcome().name(), result.message()));
		return result;
	}

	private static String stripTags(String value) {
		return value.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
	}
}
