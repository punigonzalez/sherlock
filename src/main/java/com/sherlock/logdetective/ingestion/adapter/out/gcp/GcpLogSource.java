package com.sherlock.logdetective.ingestion.adapter.out.gcp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.domain.SourceHealth;
import com.sherlock.logdetective.ingestion.domain.SourceHealth.HealthStatus;
import com.sherlock.logdetective.ingestion.domain.SourceType;
import com.sherlock.logdetective.ingestion.port.out.GcpLoggingPort;
import com.sherlock.logdetective.ingestion.port.out.GcpLoggingPort.LogEntryView;
import com.sherlock.logdetective.ingestion.port.out.GcpLoggingPort.Page;
import com.sherlock.logdetective.ingestion.port.out.LogSource;

/**
 * Cloud Logging pull source: bounded historical window + paginated polling with checkpoint cursor.
 * No Pub/Sub.
 */
public final class GcpLogSource implements LogSource {

	private final LogSourceId id;
	private final GcpLoggingPort port;
	private final String baseFilter;
	private final Duration historicalLookback;
	private final int pageSize;
	private final int maxPagesPerPoll;
	private final AtomicReference<SourceHealth> health = new AtomicReference<>(
			SourceHealth.of(HealthStatus.NOT_STARTED, "not started")
	);
	private volatile boolean running;

	public GcpLogSource(
			LogSourceId id,
			GcpLoggingPort port,
			String baseFilter,
			Duration historicalLookback,
			int pageSize,
			int maxPagesPerPoll
	) {
		this.id = Objects.requireNonNull(id);
		this.port = Objects.requireNonNull(port);
		this.baseFilter = baseFilter == null || baseFilter.isBlank() ? "severity>=ERROR" : baseFilter;
		this.historicalLookback = historicalLookback == null ? Duration.ofHours(1) : historicalLookback;
		this.pageSize = Math.max(1, pageSize);
		this.maxPagesPerPoll = Math.max(1, maxPagesPerPoll);
	}

	@Override
	public LogSourceId id() {
		return id;
	}

	@Override
	public SourceType type() {
		return SourceType.GCP_LOGGING;
	}

	@Override
	public void validate() {
		try {
			port.ping();
			health.set(SourceHealth.of(HealthStatus.HEALTHY, "gcp logging reachable"));
		}
		catch (RuntimeException ex) {
			health.set(SourceHealth.of(HealthStatus.UNAVAILABLE, ex.getMessage()));
			throw new IllegalStateException("GCP Logging validation failed: " + ex.getMessage(), ex);
		}
	}

	@Override
	public void start() {
		running = true;
		health.set(SourceHealth.of(HealthStatus.HEALTHY, "running"));
	}

	@Override
	public void stop() {
		running = false;
		health.set(SourceHealth.of(HealthStatus.STOPPED, "stopped"));
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public SourceHealth health() {
		return health.get();
	}

	@Override
	public List<RawLogRecord> fetch(FetchCursor cursor, int limit) {
		if (!running) {
			return List.of();
		}
		CursorState state = CursorState.parse(cursor);
		Instant watermark = state.watermarkInstant().orElse(Instant.now().minus(historicalLookback));
		String filter = baseFilter + " AND timestamp>\"" + watermark + "\"";
		List<RawLogRecord> out = new ArrayList<>();
		String pageToken = state.token().orElse("");
		int pages = 0;
		int capped = Math.max(1, Math.min(limit, pageSize * maxPagesPerPoll));
		try {
			while (out.size() < capped && pages < maxPagesPerPoll) {
				Page page = port.listEntries(filter, pageToken, Math.min(pageSize, capped - out.size()));
				pages++;
				for (LogEntryView entry : page.entries()) {
					out.add(toRecord(entry));
					if (out.size() >= capped) {
						break;
					}
				}
				if (!page.hasNext()) {
					pageToken = "";
					break;
				}
				pageToken = page.nextPageToken();
			}
			health.set(SourceHealth.of(HealthStatus.HEALTHY, "fetched " + out.size() + " entries"));
			return List.copyOf(out);
		}
		catch (RuntimeException ex) {
			health.set(SourceHealth.of(HealthStatus.UNAVAILABLE, ex.getMessage()));
			throw ex;
		}
	}

	@Override
	public FetchCursor advance(FetchCursor cursor, List<RawLogRecord> consumed) {
		CursorState previous = CursorState.parse(cursor);
		Instant watermark = previous.watermarkInstant().orElse(null);
		for (RawLogRecord record : consumed) {
			if (record.observedAt() == null) {
				continue;
			}
			if (watermark == null || record.observedAt().isAfter(watermark)) {
				watermark = record.observedAt();
			}
		}
		if (watermark == null) {
			watermark = Instant.now().minus(historicalLookback);
		}
		return CursorState.of(watermark, "").toCursor();
	}

	private static RawLogRecord toRecord(LogEntryView entry) {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("severity", entry.severity());
		headers.put("logName", entry.logName());
		headers.putAll(entry.labels());
		String body = entry.textPayload() == null ? "" : entry.textPayload();
		return new RawLogRecord(entry.insertId(), entry.timestamp(), body, headers);
	}

	record CursorState(Instant watermark, String pageToken) {
		static CursorState parse(FetchCursor cursor) {
			if (cursor == null || cursor.isStart()) {
				return new CursorState(null, "");
			}
			Instant watermark = null;
			String token = "";
			for (String part : cursor.opaque().split("\\|")) {
				if (part.startsWith("ts=")) {
					String v = part.substring(3);
					if (!v.isBlank()) {
						watermark = Instant.parse(v);
					}
				}
				else if (part.startsWith("token=")) {
					token = part.substring(6);
				}
			}
			return new CursorState(watermark, token);
		}

		static CursorState of(Instant watermark, String token) {
			return new CursorState(watermark, token == null ? "" : token);
		}

		Optional<Instant> watermarkInstant() {
			return Optional.ofNullable(watermark);
		}

		Optional<String> token() {
			return pageToken == null || pageToken.isBlank() ? Optional.empty() : Optional.of(pageToken);
		}

		FetchCursor toCursor() {
			String ts = watermark == null ? "" : watermark.toString();
			return new FetchCursor("ts=" + ts + "|token=" + (pageToken == null ? "" : pageToken));
		}
	}
}
