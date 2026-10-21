package com.sherlock.logdetective.ingestion.adapter.out.gcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.port.out.GcpLoggingPort;
import com.sherlock.logdetective.ingestion.port.out.LogSource;
import com.sherlock.logdetective.ingestion.port.out.LogSource.FetchCursor;
import com.sherlock.logdetective.ingestion.port.out.LogSource.RawLogRecord;

class GcpLogSourceTest {

	@Test
	void fetchUsesFilterAndAdvancesWatermark() {
		FakePort port = new FakePort();
		GcpLogSource source = new GcpLogSource(
				new LogSourceId("gcp"),
				port,
				"severity>=ERROR",
				Duration.ofHours(1),
				10,
				2
		);
		source.start();

		List<RawLogRecord> batch = source.fetch(FetchCursor.start(), 10);
		assertThat(batch).hasSize(2);
		assertThat(port.lastFilter).contains("severity>=ERROR").contains("timestamp>");

		FetchCursor next = source.advance(FetchCursor.start(), batch);
		assertThat(next.opaque()).contains("ts=" + Instant.parse("2026-10-21T12:00:02Z"));

		List<RawLogRecord> second = source.fetch(next, 10);
		assertThat(second).isEmpty();
	}

	@Test
	void validateSurfacesPingFailures() {
		GcpLoggingPort failing = new GcpLoggingPort() {
			@Override
			public Page listEntries(String filter, String pageToken, int pageSize) {
				return new Page(List.of(), "");
			}

			@Override
			public void ping() {
				throw new IllegalStateException("adc missing");
			}
		};
		GcpLogSource source = new GcpLogSource(
				new LogSourceId("gcp"),
				failing,
				"severity>=ERROR",
				Duration.ofMinutes(5),
				10,
				1
		);
		assertThatThrownBy(source::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("adc missing");
	}

	@Test
	void respectsMaxPagesPerPoll() {
		FakePort port = new FakePort();
		port.pages.clear();
		port.pages.add(new GcpLoggingPort.Page(List.of(
				entry("a", Instant.parse("2026-10-21T12:00:01Z"))
		), "t1"));
		port.pages.add(new GcpLoggingPort.Page(List.of(
				entry("b", Instant.parse("2026-10-21T12:00:02Z"))
		), "t2"));
		port.pages.add(new GcpLoggingPort.Page(List.of(
				entry("c", Instant.parse("2026-10-21T12:00:03Z"))
		), ""));
		GcpLogSource source = new GcpLogSource(
				new LogSourceId("gcp"),
				port,
				"severity>=ERROR",
				Duration.ofHours(1),
				10,
				2
		);
		source.start();
		assertThat(source.fetch(FetchCursor.start(), 100)).hasSize(2);
		assertThat(port.listCalls.get()).isEqualTo(2);
	}

	private static GcpLoggingPort.LogEntryView entry(String id, Instant ts) {
		return new GcpLoggingPort.LogEntryView(id, ts, "ERROR", "projects/p/logs/app", "boom " + id, Map.of());
	}

	private static final class FakePort implements GcpLoggingPort {
		private final List<Page> pages = new ArrayList<>();
		private final AtomicInteger listCalls = new AtomicInteger();
		private String lastFilter;

		FakePort() {
			pages.add(new Page(List.of(
					entry("1", Instant.parse("2026-10-21T12:00:01Z")),
					entry("2", Instant.parse("2026-10-21T12:00:02Z"))
			), ""));
		}

		@Override
		public Page listEntries(String filter, String pageToken, int pageSize) {
			lastFilter = filter;
			int idx = listCalls.getAndIncrement();
			if (idx < pages.size()) {
				return pages.get(idx);
			}
			return new Page(List.of(), "");
		}

		@Override
		public void ping() {
			// ok
		}
	}
}
