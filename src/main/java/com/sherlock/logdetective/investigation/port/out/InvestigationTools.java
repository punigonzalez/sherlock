package com.sherlock.logdetective.investigation.port.out;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bounded read-only tools available to AI investigators.
 */
public interface InvestigationTools {

	ToolResult getIncidentDetails(UUID incidentId);

	ToolResult getRelatedErrors(UUID incidentId, int limit);

	ToolResult getRecentEvents(int limit);

	ToolResult searchLogs(String query, int limit);

	ToolResult getTraceDetails(String traceId, int limit);

	ToolResult getServiceMetrics(String service);

	enum ToolOutcome {
		OK,
		UNSUPPORTED,
		NOT_FOUND,
		ERROR
	}

	record ToolResult(
			String tool,
			ToolOutcome outcome,
			String message,
			List<Map<String, String>> rows
	) {
		public static ToolResult ok(String tool, String message, List<Map<String, String>> rows) {
			return new ToolResult(tool, ToolOutcome.OK, message, rows == null ? List.of() : List.copyOf(rows));
		}

		public static ToolResult unsupported(String tool, String message) {
			return new ToolResult(tool, ToolOutcome.UNSUPPORTED, message, List.of());
		}

		public static ToolResult notFound(String tool, String message) {
			return new ToolResult(tool, ToolOutcome.NOT_FOUND, message, List.of());
		}

		public static ToolResult error(String tool, String message) {
			return new ToolResult(tool, ToolOutcome.ERROR, message, List.of());
		}
	}
}
