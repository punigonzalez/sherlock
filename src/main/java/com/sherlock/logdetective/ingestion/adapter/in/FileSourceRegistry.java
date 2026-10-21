package com.sherlock.logdetective.ingestion.adapter.in;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sherlock.logdetective.ingestion.adapter.out.file.FileLogSource;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.domain.SourceHealth;
import com.sherlock.logdetective.ingestion.port.out.LogSource;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class FileSourceRegistry {

	private static final Logger log = LoggerFactory.getLogger(FileSourceRegistry.class);

	private final FileSourceProperties properties;
	private final Map<String, LogSource> sources = new ConcurrentHashMap<>();

	public FileSourceRegistry(FileSourceProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	public void startEnabledSources() {
		if (!properties.isEnabled()) {
			log.info("Log ingestion disabled (logdetective.ingestion.enabled=false)");
			return;
		}
		for (FileSourceProperties.SourceConfig config : properties.getSources()) {
			if (!config.isEnabled()) {
				continue;
			}
			if (!"FILE".equalsIgnoreCase(config.getType())) {
				log.warn("Skipping unsupported source type {} for id={}", config.getType(), config.getId());
				continue;
			}
			if (config.getId() == null || config.getId().isBlank() || config.getPath() == null || config.getPath().isBlank()) {
				log.warn("Skipping invalid source config (id/path required)");
				continue;
			}
			FileLogSource source = new FileLogSource(
					new LogSourceId(config.getId()),
					Path.of(config.getPath()),
					config.charset()
			);
			try {
				source.validate();
				source.start();
				sources.put(config.getId(), source);
				log.info("Started file log source id={} path={}", config.getId(), config.getPath());
			}
			catch (RuntimeException ex) {
				log.error("Failed to start file log source id={}: {}", config.getId(), ex.getMessage());
			}
		}
	}

	@PreDestroy
	public void stopAll() {
		sources.values().forEach(LogSource::stop);
		sources.clear();
	}

	public List<LogSource> runningSources() {
		return List.copyOf(sources.values());
	}

	public List<SourceHealthView> health() {
		List<SourceHealthView> views = new ArrayList<>();
		sources.forEach((id, source) -> {
			SourceHealth health = source.health();
			views.add(new SourceHealthView(id, health.status().name(), health.detail()));
		});
		return views;
	}

	public record SourceHealthView(String sourceId, String status, String detail) {
	}
}
