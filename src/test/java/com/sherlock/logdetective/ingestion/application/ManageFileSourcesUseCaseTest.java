package com.sherlock.logdetective.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sherlock.logdetective.ingestion.adapter.out.file.FileLogSource;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase.SourceRegistryPort;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase.SourceView;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase.ValidationResult;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.domain.SafePathValidator;
import com.sherlock.logdetective.ingestion.port.out.LogSource;

class ManageFileSourcesUseCaseTest {

	@TempDir
	Path tempDir;

	private ManageFileSourcesUseCase useCase;
	private InMemoryRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new InMemoryRegistry();
		useCase = new ManageFileSourcesUseCase(
				new SafePathValidator(tempDir),
				registry,
				mock(IngestLogsUseCase.class),
				10
		);
	}

	@Test
	void validateAcceptsReadableFileUnderAllowlist() throws Exception {
		Path log = tempDir.resolve("app.log");
		Files.writeString(log, "hello\n");

		ValidationResult result = useCase.validate("app", log.toString(), "UTF-8");

		assertThat(result.valid()).isTrue();
		assertThat(result.sourceId()).isEqualTo("app");
		assertThat(result.resolvedPath()).isEqualTo(log.toAbsolutePath().normalize().toString());
	}

	@Test
	void registerAndStartTracksSource() throws Exception {
		Path log = tempDir.resolve("app.log");
		Files.writeString(log, "line\n");

		SourceView view = useCase.registerAndStart("app", "app.log", "UTF-8");

		assertThat(view.running()).isTrue();
		assertThat(useCase.list()).hasSize(1);
		assertThat(useCase.health("app")).isPresent();
	}

	@Test
	void rejectsPathOutsideAllowlist() {
		assertThatThrownBy(() -> useCase.validate("x", "/tmp/outside.log", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("allowlist");
	}

	private static final class InMemoryRegistry implements SourceRegistryPort {
		private final ConcurrentHashMap<String, LogSource> sources = new ConcurrentHashMap<>();
		private final ConcurrentHashMap<String, Path> paths = new ConcurrentHashMap<>();

		@Override
		public LogSource createUnstarted(LogSourceId id, Path path, Charset charset) {
			return new FileLogSource(id, path, charset == null ? StandardCharsets.UTF_8 : charset);
		}

		@Override
		public LogSource registerStarted(LogSourceId id, Path path, Charset charset) {
			FileLogSource source = new FileLogSource(id, path, charset == null ? StandardCharsets.UTF_8 : charset);
			source.validate();
			source.start();
			sources.put(id.value(), source);
			paths.put(id.value(), path);
			return source;
		}

		@Override
		public Optional<LogSource> find(LogSourceId id) {
			return Optional.ofNullable(sources.get(id.value()));
		}

		@Override
		public Optional<Path> pathOf(LogSourceId id) {
			return Optional.ofNullable(paths.get(id.value()));
		}

		@Override
		public List<LogSource> runningSources() {
			return new ArrayList<>(sources.values());
		}
	}
}
