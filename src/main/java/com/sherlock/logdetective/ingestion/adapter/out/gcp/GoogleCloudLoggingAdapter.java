package com.sherlock.logdetective.ingestion.adapter.out.gcp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.Logging.EntryListOption;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload;
import com.google.cloud.logging.Severity;
import com.sherlock.logdetective.ingestion.port.out.GcpLoggingPort;

/**
 * Official Cloud Logging client using Application Default Credentials (ADC).
 */
public final class GoogleCloudLoggingAdapter implements GcpLoggingPort, AutoCloseable {

	private final Logging logging;
	private final String projectId;

	public GoogleCloudLoggingAdapter(String projectId) {
		this(projectId, LoggingOptions.newBuilder()
				.setProjectId(Objects.requireNonNull(projectId, "projectId"))
				.build()
				.getService());
	}

	GoogleCloudLoggingAdapter(String projectId, Logging logging) {
		this.projectId = Objects.requireNonNull(projectId, "projectId");
		this.logging = Objects.requireNonNull(logging, "logging");
	}

	@Override
	public Page listEntries(String filter, String pageToken, int pageSize) {
		List<EntryListOption> options = new ArrayList<>();
		options.add(EntryListOption.filter(filter == null ? "" : filter));
		options.add(EntryListOption.pageSize(Math.max(1, Math.min(pageSize, 1000))));
		if (pageToken != null && !pageToken.isBlank()) {
			options.add(EntryListOption.pageToken(pageToken));
		}
		var page = logging.listLogEntries(options.toArray(EntryListOption[]::new));
		List<LogEntryView> views = new ArrayList<>();
		for (LogEntry entry : page.getValues()) {
			views.add(toView(entry));
		}
		String next = page.hasNextPage() ? page.getNextPageToken() : "";
		return new Page(views, next);
	}

	@Override
	public void ping() {
		// Lightweight metadata call to validate ADC / project reachability.
		logging.listLogs(Logging.ListOption.pageSize(1));
	}

	public String projectId() {
		return projectId;
	}

	@Override
	public void close() {
		try {
			logging.close();
		}
		catch (Exception ignored) {
			// best-effort
		}
	}

	private static LogEntryView toView(LogEntry entry) {
		Instant ts = entry.getInstantTimestamp() == null ? Instant.EPOCH : entry.getInstantTimestamp();
		String text = "";
		Payload<?> payload = entry.getPayload();
		if (payload != null) {
			Object data = payload.getData();
			text = data == null ? "" : String.valueOf(data);
		}
		Severity severity = entry.getSeverity();
		Map<String, String> labels = new HashMap<>();
		if (entry.getLabels() != null) {
			labels.putAll(entry.getLabels());
		}
		String insertId = entry.getInsertId() == null || entry.getInsertId().isBlank()
				? ts + ":" + Integer.toHexString(Objects.hash(text, entry.getLogName()))
				: entry.getInsertId();
		return new LogEntryView(
				insertId,
				ts,
				severity == null ? "DEFAULT" : severity.name(),
				entry.getLogName() == null ? "" : entry.getLogName(),
				text,
				labels
		);
	}
}
