package com.sherlock.logdetective.ingestion.adapter.in;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.sherlock.logdetective.ingestion.adapter.out.gcp.GcpLogSource;
import com.sherlock.logdetective.ingestion.adapter.out.gcp.GoogleCloudLoggingAdapter;
import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.domain.SourceHealth;
import com.sherlock.logdetective.ingestion.port.out.GcpLoggingPort;
import com.sherlock.logdetective.ingestion.port.out.LogSource;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
@ConditionalOnProperty(prefix = "logdetective.gcp-logging", name = "enabled", havingValue = "true")
public class GcpLoggingSourceRegistry {

	private static final Logger log = LoggerFactory.getLogger(GcpLoggingSourceRegistry.class);

	private final GcpLoggingProperties properties;
	private final AtomicReference<GcpLogSource> source = new AtomicReference<>();
	private final AtomicReference<GoogleCloudLoggingAdapter> adapter = new AtomicReference<>();

	public GcpLoggingSourceRegistry(GcpLoggingProperties properties) {
		this.properties = properties;
	}

	@PostConstruct
	public void start() {
		String projectId = properties.getProjectId();
		if (projectId == null || projectId.isBlank()) {
			throw new IllegalStateException("logdetective.gcp-logging.project-id is required when enabled");
		}
		GoogleCloudLoggingAdapter gcp = new GoogleCloudLoggingAdapter(projectId);
		adapter.set(gcp);
		GcpLogSource logSource = new GcpLogSource(
				new LogSourceId(properties.getSourceId()),
				gcp,
				properties.getFilter(),
				properties.getHistoricalLookback(),
				properties.getPageSize(),
				properties.getMaxPagesPerPoll()
		);
		try {
			logSource.validate();
			logSource.start();
			source.set(logSource);
			log.info("Started GCP Logging source id={} project={}", properties.getSourceId(), projectId);
		}
		catch (RuntimeException ex) {
			gcp.close();
			throw ex;
		}
	}

	@PreDestroy
	public void stop() {
		GcpLogSource current = source.getAndSet(null);
		if (current != null) {
			current.stop();
		}
		GoogleCloudLoggingAdapter gcp = adapter.getAndSet(null);
		if (gcp != null) {
			gcp.close();
		}
	}

	public Optional<LogSource> source() {
		return Optional.ofNullable(source.get());
	}

	public SourceHealth health() {
		return source.get() == null
				? SourceHealth.of(SourceHealth.HealthStatus.NOT_STARTED, "not started")
				: source.get().health();
	}

	/** Package-visible for tests that inject a port without ADC. */
	void bindForTests(GcpLogSource testSource) {
		source.set(testSource);
	}

	public GcpLoggingPort portOrNull() {
		return adapter.get();
	}
}
