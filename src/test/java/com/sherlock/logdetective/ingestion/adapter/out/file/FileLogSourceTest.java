package com.sherlock.logdetective.ingestion.adapter.out.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.domain.SourceHealth.HealthStatus;
import com.sherlock.logdetective.ingestion.port.out.LogSource.FetchCursor;
import com.sherlock.logdetective.ingestion.port.out.LogSource.RawLogRecord;

class FileLogSourceTest {

	@TempDir
	Path tempDir;

	@Test
	void readsAppendAndCheckpoints() throws Exception {
		Path log = tempDir.resolve("app.log");
		Files.writeString(log, "line-1\n");
		FileLogSource source = new FileLogSource(new LogSourceId("f1"), log);
		source.start();

		List<RawLogRecord> first = source.fetch(FetchCursor.start(), 10);
		assertThat(first).hasSize(1);
		assertThat(first.getFirst().rawBody()).isEqualTo("line-1");
		FetchCursor cursor = source.advance(FetchCursor.start(), first);

		Files.writeString(log, "line-1\nline-2\n");
		List<RawLogRecord> second = source.fetch(cursor, 10);
		assertThat(second).extracting(RawLogRecord::rawBody).containsExactly("line-2");
	}

	@Test
	void handlesTruncationByRewinding() throws Exception {
		Path log = tempDir.resolve("app.log");
		Files.writeString(log, "aaaaaaaa\nbbbbbbbb\n");
		FileLogSource source = new FileLogSource(new LogSourceId("f1"), log);
		source.start();
		List<RawLogRecord> first = source.fetch(FetchCursor.start(), 10);
		FetchCursor cursor = source.advance(FetchCursor.start(), first);

		Files.writeString(log, "short\n");
		List<RawLogRecord> after = source.fetch(cursor, 10);
		assertThat(after).extracting(RawLogRecord::rawBody).containsExactly("short");
		assertThat(source.health().status()).isIn(HealthStatus.HEALTHY, HealthStatus.DEGRADED);
	}

	@Test
	void assemblesJavaStackMultiline() throws Exception {
		Path log = tempDir.resolve("app.log");
		Files.writeString(log, """
				ERROR boom
				java.lang.RuntimeException: x
					at com.example.App.main(App.java:1)
				INFO ok
				""", StandardCharsets.UTF_8);
		FileLogSource source = new FileLogSource(new LogSourceId("f1"), log);
		source.start();
		List<RawLogRecord> records = source.fetch(FetchCursor.start(), 10);
		assertThat(records).hasSize(2);
		assertThat(records.getFirst().rawBody()).contains("at com.example.App.main");
		assertThat(records.get(1).rawBody()).isEqualTo("INFO ok");
	}

	@Test
	void keepsPartialLineUntilNewline() throws Exception {
		Path log = tempDir.resolve("app.log");
		Files.writeString(log, "partial");
		FileLogSource source = new FileLogSource(new LogSourceId("f1"), log);
		source.start();
		assertThat(source.fetch(FetchCursor.start(), 10)).isEmpty();
		FetchCursor cursor = source.advance(FetchCursor.start(), List.of());

		Files.writeString(log, "partial-complete\n");
		List<RawLogRecord> records = source.fetch(cursor, 10);
		assertThat(records).extracting(RawLogRecord::rawBody).containsExactly("partial-complete");
	}

	@Test
	void deletionMarksUnavailable() throws Exception {
		Path log = tempDir.resolve("app.log");
		Files.writeString(log, "x\n");
		FileLogSource source = new FileLogSource(new LogSourceId("f1"), log);
		source.start();
		Files.delete(log);
		assertThat(source.fetch(FetchCursor.start(), 10)).isEmpty();
		assertThat(source.health().status()).isEqualTo(HealthStatus.UNAVAILABLE);
	}
}
