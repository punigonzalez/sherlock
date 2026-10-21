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
@ConditionalOnProperty(prefix = "logdetective.ingestion", name = "enabled", havingValue = "true")
public class FileIngestionScheduler {

	private static final Logger log = LoggerFactory.getLogger(FileIngestionScheduler.class);

	private final FileSourceRegistry registry;
	private final FileSourceProperties properties;
	private final IngestLogsUseCase ingestLogsUseCase;

	public FileIngestionScheduler(
			FileSourceRegistry registry,
			FileSourceProperties properties,
			IngestLogsUseCase ingestLogsUseCase
	) {
		this.registry = registry;
		this.properties = properties;
		this.ingestLogsUseCase = ingestLogsUseCase;
	}

	@Scheduled(fixedDelayString = "${logdetective.ingestion.poll-interval-ms:2000}")
	public void poll() {
		for (LogSource source : registry.runningSources()) {
			try {
				IngestBatchResult result = ingestLogsUseCase.ingest(source, properties.getBatchSize());
				if (result.fetched() > 0) {
					log.info(
							"Ingested source={} fetched={} accepted={} dupes={} parseFailures={} errors={}",
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
				log.warn("Ingest failed for source={}: {}", source.id().value(), ex.getMessage());
			}
		}
	}
}
