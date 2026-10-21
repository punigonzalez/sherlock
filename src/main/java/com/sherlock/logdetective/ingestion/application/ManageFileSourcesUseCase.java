package com.sherlock.logdetective.ingestion.application;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.domain.SafePathValidator;
import com.sherlock.logdetective.ingestion.domain.SourceHealth;
import com.sherlock.logdetective.ingestion.domain.SourceType;
import com.sherlock.logdetective.ingestion.port.out.LogSource;

/**
 * Application facade for validating/registering file sources and triggering ingest.
 * Registry mutations are delegated via {@link SourceRegistryPort}.
 */
public final class ManageFileSourcesUseCase {

	private final SafePathValidator pathValidator;
	private final SourceRegistryPort registry;
	private final IngestLogsUseCase ingestLogsUseCase;
	private final int defaultBatchSize;

	public ManageFileSourcesUseCase(
			SafePathValidator pathValidator,
			SourceRegistryPort registry,
			IngestLogsUseCase ingestLogsUseCase,
			int defaultBatchSize
	) {
		this.pathValidator = Objects.requireNonNull(pathValidator);
		this.registry = Objects.requireNonNull(registry);
		this.ingestLogsUseCase = Objects.requireNonNull(ingestLogsUseCase);
		this.defaultBatchSize = Math.max(1, defaultBatchSize);
	}

	public ValidationResult validate(String id, String path, String encoding) {
		Objects.requireNonNull(id, "id");
		Path allowed = pathValidator.resolveAllowed(path);
		Charset charset = charsetOf(encoding);
		LogSource source = registry.createUnstarted(new LogSourceId(id), allowed, charset);
		try {
			source.validate();
			SourceHealth health = source.health();
			return new ValidationResult(true, id, allowed.toString(), health.status().name(), health.detail(), null);
		}
		catch (RuntimeException ex) {
			return new ValidationResult(false, id, allowed.toString(), "INVALID", ex.getMessage(), ex.getMessage());
		}
	}

	public SourceView registerAndStart(String id, String path, String encoding) {
		ValidationResult validation = validate(id, path, encoding);
		if (!validation.valid()) {
			throw new IllegalArgumentException(validation.errorMessage() == null ? "invalid source" : validation.errorMessage());
		}
		Path allowed = pathValidator.resolveAllowed(path);
		LogSource source = registry.registerStarted(new LogSourceId(id), allowed, charsetOf(encoding));
		SourceHealth health = source.health();
		return new SourceView(id, SourceType.FILE.name(), allowed.toString(), true, health.status().name(), health.detail());
	}

	public List<SourceView> list() {
		List<SourceView> views = new ArrayList<>();
		for (LogSource source : registry.runningSources()) {
			SourceHealth health = source.health();
			String path = registry.pathOf(source.id()).map(Path::toString).orElse("");
			views.add(new SourceView(
					source.id().value(),
					source.type().name(),
					path,
					source.isRunning(),
					health.status().name(),
					health.detail()
			));
		}
		return List.copyOf(views);
	}

	public Optional<SourceView> health(String id) {
		return registry.find(new LogSourceId(id)).map(source -> {
			SourceHealth health = source.health();
			String path = registry.pathOf(source.id()).map(Path::toString).orElse("");
			return new SourceView(
					source.id().value(),
					source.type().name(),
					path,
					source.isRunning(),
					health.status().name(),
					health.detail()
			);
		});
	}

	public IngestBatchResult ingestOnce(String id) {
		LogSource source = registry.find(new LogSourceId(id))
				.orElseThrow(() -> new IllegalArgumentException("unknown source: " + id));
		return ingestLogsUseCase.ingest(source, defaultBatchSize);
	}

	private static Charset charsetOf(String encoding) {
		if (encoding == null || encoding.isBlank()) {
			return StandardCharsets.UTF_8;
		}
		return Charset.forName(encoding);
	}

	public record ValidationResult(
			boolean valid,
			String sourceId,
			String resolvedPath,
			String status,
			String detail,
			String errorMessage
	) {
	}

	public record SourceView(
			String id,
			String type,
			String path,
			boolean running,
			String healthStatus,
			String healthDetail
	) {
	}

	/**
	 * Port so API/CLI do not depend on the Spring registry adapter directly.
	 */
	public interface SourceRegistryPort {
		LogSource createUnstarted(LogSourceId id, Path path, Charset charset);

		LogSource registerStarted(LogSourceId id, Path path, Charset charset);

		Optional<LogSource> find(LogSourceId id);

		Optional<Path> pathOf(LogSourceId id);

		List<LogSource> runningSources();
	}
}
