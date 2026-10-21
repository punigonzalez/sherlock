package com.sherlock.logdetective.investigation.adapter.out.tools;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.sherlock.logdetective.correlation.domain.Evidence;
import com.sherlock.logdetective.correlation.port.out.EvidenceRepository;
import com.sherlock.logdetective.detection.domain.Incident;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;
import com.sherlock.logdetective.ingestion.domain.CanonicalEvent;
import com.sherlock.logdetective.ingestion.domain.redaction.RedactionHook;
import com.sherlock.logdetective.ingestion.port.out.EventRepository;
import com.sherlock.logdetective.investigation.domain.PromptSanitizer;
import com.sherlock.logdetective.investigation.port.out.InvestigationTools;

/**
 * Read-only tool implementations over existing repositories. Bounded and redacted.
 */
public final class RepositoryInvestigationTools implements InvestigationTools {

	private final IncidentRepository incidentRepository;
	private final EvidenceRepository evidenceRepository;
	private final EventRepository eventRepository;
	private final RedactionHook redactionHook;
	private final PromptSanitizer sanitizer;
	private final int hardLimit;

	public RepositoryInvestigationTools(
			IncidentRepository incidentRepository,
			EvidenceRepository evidenceRepository,
			EventRepository eventRepository,
			RedactionHook redactionHook,
			PromptSanitizer sanitizer,
			int hardLimit
	) {
		this.incidentRepository = Objects.requireNonNull(incidentRepository);
		this.evidenceRepository = Objects.requireNonNull(evidenceRepository);
		this.eventRepository = Objects.requireNonNull(eventRepository);
		this.redactionHook = Objects.requireNonNull(redactionHook);
		this.sanitizer = Objects.requireNonNull(sanitizer);
		this.hardLimit = Math.max(1, Math.min(hardLimit, 50));
	}

	@Override
	public ToolResult getIncidentDetails(UUID incidentId) {
		return incidentRepository.findById(incidentId)
				.map(this::incidentRow)
				.map(row -> ToolResult.ok("getIncidentDetails", "incident found", List.of(row)))
				.orElseGet(() -> ToolResult.notFound("getIncidentDetails", "incident not found"));
	}

	@Override
	public ToolResult getRelatedErrors(UUID incidentId, int limit) {
		var incident = incidentRepository.findById(incidentId);
		if (incident.isEmpty()) {
			return ToolResult.notFound("getRelatedErrors", "incident not found");
		}
		List<Evidence> evidence = evidenceRepository.findByIncidentId(incidentId);
		List<Map<String, String>> rows = new ArrayList<>();
		int capped = cap(limit);
		for (Evidence e : evidence) {
			if (rows.size() >= capped) {
				break;
			}
			Map<String, String> row = new LinkedHashMap<>();
			row.put("evidenceId", e.id().value().toString());
			row.put("kind", e.kind().name());
			row.put("strength", e.strength().name());
			row.put("reason", sanitize("evidence", e.reason()));
			row.put("summary", sanitize("evidence", e.summary()));
			rows.add(row);
		}
		Incident i = incident.get();
		rows.add(0, Map.of(
				"fingerprint", i.fingerprint().value(),
				"title", sanitize("incident", i.title()),
				"occurrenceCount", String.valueOf(i.occurrenceCount())
		));
		return ToolResult.ok("getRelatedErrors", "related evidence and fingerprint", rows);
	}

	@Override
	public ToolResult getRecentEvents(int limit) {
		Instant to = Instant.now();
		Instant from = to.minusSeconds(3600);
		List<CanonicalEvent> events = eventRepository.findBetween(from, to);
		return ToolResult.ok("getRecentEvents", "events in last hour", toRows(events, limit));
	}

	@Override
	public ToolResult searchLogs(String query, int limit) {
		if (query == null || query.isBlank()) {
			return ToolResult.error("searchLogs", "query must not be blank");
		}
		String needle = query.toLowerCase(Locale.ROOT);
		Instant to = Instant.now();
		Instant from = to.minusSeconds(6 * 3600);
		List<CanonicalEvent> matches = eventRepository.findBetween(from, to).stream()
				.filter(e -> e.message() != null && e.message().toLowerCase(Locale.ROOT).contains(needle))
				.limit(cap(limit))
				.toList();
		return ToolResult.ok("searchLogs", "substring match over last 6h", toRows(matches, limit));
	}

	@Override
	public ToolResult getTraceDetails(String traceId, int limit) {
		if (traceId == null || traceId.isBlank()) {
			return ToolResult.error("getTraceDetails", "traceId required");
		}
		Instant to = Instant.now();
		Instant from = to.minusSeconds(6 * 3600);
		List<CanonicalEvent> matches = eventRepository.findBetween(from, to).stream()
				.filter(e -> e.traceId().filter(traceId::equals).isPresent())
				.limit(cap(limit))
				.toList();
		if (matches.isEmpty()) {
			return ToolResult.notFound("getTraceDetails", "no events for traceId");
		}
		return ToolResult.ok("getTraceDetails", "events sharing traceId", toRows(matches, limit));
	}

	@Override
	public ToolResult getServiceMetrics(String service) {
		if (service == null || service.isBlank()) {
			return ToolResult.unsupported(
					"getServiceMetrics",
					"service metrics require aggregated telemetry; not available in MVP event store"
			);
		}
		Instant to = Instant.now();
		Instant from = to.minusSeconds(3600);
		long count = eventRepository.findBetween(from, to).stream()
				.filter(e -> e.service().filter(service::equals).isPresent())
				.count();
		if (count == 0) {
			return ToolResult.unsupported(
					"getServiceMetrics",
					"no precomputed metrics; only raw event count would be available and was empty for service="
							+ service
			);
		}
		return ToolResult.ok(
				"getServiceMetrics",
				"best-effort raw event count (not a true metric series)",
				List.of(Map.of(
						"service", service,
						"windowSeconds", "3600",
						"eventCount", String.valueOf(count),
						"note", "not a full metrics backend"
				))
		);
	}

	private Map<String, String> incidentRow(Incident i) {
		Map<String, String> row = new LinkedHashMap<>();
		row.put("id", i.id().toString());
		row.put("status", i.status().name());
		row.put("severity", i.severity().name());
		row.put("title", sanitize("incident", i.title()));
		row.put("fingerprint", i.fingerprint().value());
		row.put("occurrenceCount", String.valueOf(i.occurrenceCount()));
		row.put("firstSeenAt", i.firstSeenAt().toString());
		row.put("lastSeenAt", i.lastSeenAt().toString());
		return row;
	}

	private List<Map<String, String>> toRows(List<CanonicalEvent> events, int limit) {
		List<Map<String, String>> rows = new ArrayList<>();
		for (CanonicalEvent e : events) {
			if (rows.size() >= cap(limit)) {
				break;
			}
			Map<String, String> row = new LinkedHashMap<>();
			row.put("id", e.id().toString());
			row.put("level", e.level().name());
			row.put("service", e.service().orElse(""));
			row.put("traceId", e.traceId().orElse(""));
			row.put("timestamp", e.timestamp().toString());
			row.put("message", sanitize("event", e.message()));
			rows.add(row);
		}
		return rows;
	}

	private String sanitize(String label, String raw) {
		return sanitizer.wrapUntrusted(label, redactionHook.redact(raw == null ? "" : raw));
	}

	private int cap(int limit) {
		if (limit < 1) {
			return hardLimit;
		}
		return Math.min(limit, hardLimit);
	}
}
