package com.sherlock.logdetective.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.sherlock.logdetective.detection.application.RecordErrorObservationUseCase;
import com.sherlock.logdetective.detection.domain.fingerprint.ErrorFingerprintCalculator;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;
import com.sherlock.logdetective.ingestion.adapter.in.FileSourceProperties;
import com.sherlock.logdetective.ingestion.application.IngestLogsUseCase;
import com.sherlock.logdetective.ingestion.domain.parse.CompositeLogParser;
import com.sherlock.logdetective.ingestion.domain.parse.LogParser;
import com.sherlock.logdetective.ingestion.domain.redaction.BasicRedactionHook;
import com.sherlock.logdetective.ingestion.domain.redaction.RedactionHook;
import com.sherlock.logdetective.ingestion.port.out.EventRepository;
import com.sherlock.logdetective.ingestion.port.out.SourceCheckpointStore;
import com.sherlock.logdetective.investigation.adapter.out.NoOpAiInvestigator;
import com.sherlock.logdetective.investigation.application.InvestigateIncidentUseCase;
import com.sherlock.logdetective.investigation.port.out.AiInvestigator;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(FileSourceProperties.class)
public class LogDetectiveConfig {

	@Bean
	AiInvestigator aiInvestigator() {
		return new NoOpAiInvestigator();
	}

	@Bean
	InvestigateIncidentUseCase investigateIncidentUseCase(AiInvestigator aiInvestigator) {
		return new InvestigateIncidentUseCase(aiInvestigator);
	}

	@Bean
	ErrorFingerprintCalculator errorFingerprintCalculator() {
		return new ErrorFingerprintCalculator();
	}

	@Bean
	RecordErrorObservationUseCase recordErrorObservationUseCase(
			IncidentRepository incidentRepository,
			ErrorFingerprintCalculator errorFingerprintCalculator
	) {
		return new RecordErrorObservationUseCase(incidentRepository, errorFingerprintCalculator);
	}

	@Bean
	LogParser logParser() {
		return new CompositeLogParser();
	}

	@Bean
	RedactionHook redactionHook() {
		return new BasicRedactionHook();
	}

	@Bean
	IngestLogsUseCase ingestLogsUseCase(
			EventRepository eventRepository,
			SourceCheckpointStore checkpointStore,
			LogParser logParser,
			RedactionHook redactionHook,
			RecordErrorObservationUseCase recordErrorObservationUseCase
	) {
		return new IngestLogsUseCase(
				eventRepository,
				checkpointStore,
				logParser,
				redactionHook,
				recordErrorObservationUseCase
		);
	}
}
