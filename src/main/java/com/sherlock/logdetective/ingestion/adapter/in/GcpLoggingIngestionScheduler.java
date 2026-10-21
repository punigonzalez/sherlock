package com.sherlock.logdetective.ingestion.adapter.in;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sherlock.logdetective.ingestion.application.IngestBatchResult;
import com.sherlock.logdetective.ingestion.application.IngestLogsUseCase;
import com.sherlock.logdetective.ingestion.port.out.LogSource;

@Component
@ConditionalOnProperty(prefix = "logdetective.gcp-logging", name = "enabled", havingValue = "true")
public class GcpLoggingIngestionScheduler {

	private static final Logger log = LoggerFactory.getLogger(GcpLoggingIngestionScheduler.class);

	private final GcpLoggingSourceRegistry registry;
	private final GcpLoggingProperties properties;
	private final IngestLogsUseCase ingestLogsUseCase;

	public GcpLoggingIngestionScheduler(
			GcpLoggingSourceRegistry registry,
			GcpLoggingProperties properties,
			IngestLogsUseCase ingestLogsUseCase
	) {
		this.registry = registry;
		this.properties = properties;
		this.ingestLogsUseCase = ingestLogsUseCase;
	}

	@Scheduled(fixedDelayString = "${logdetective.gcp-logging.poll-interval-ms:10000}")
	public void poll() {
		LogSource source = registry.source().orElse(null);
		if (source == null || !source.isRunning()) {
			return;
		}
		try {
			IngestBatchResult result = ingestLogsUseCase.ingest(source, properties.getPageSize());
			if (result.fetched() > 0) {
				log.info(
						"GCP ingest source={} fetched={} accepted={} dupes={} parseFailures={} errors={}",
						source.id().value(),
						result.fetched(),
						result.accepted(),
						result.duplicates(),
						result.parseFailures(),
						result.errorsRoutedToIncidents()
				);
			}
		}
		catch (RuntimeException ex) {
			log.warn("GCP ingest failed for source={}: {}", source.id().value(), ex.getMessage());
		}
	}
}
