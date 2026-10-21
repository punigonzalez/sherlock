package com.sherlock.logdetective.shared.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.sherlock.logdetective.cli.CliFacade;
import com.sherlock.logdetective.correlation.application.CorrelateEvidenceUseCase;
import com.sherlock.logdetective.correlation.application.QueryEvidenceUseCase;
import com.sherlock.logdetective.correlation.port.out.EvidenceRepository;
import com.sherlock.logdetective.detection.adapter.in.DetectionProperties;
import com.sherlock.logdetective.detection.application.EvaluateDetectionsUseCase;
import com.sherlock.logdetective.detection.application.QueryIncidentsUseCase;
import com.sherlock.logdetective.detection.application.RecordErrorObservationUseCase;
import com.sherlock.logdetective.detection.domain.DetectionRulesConfig;
import com.sherlock.logdetective.detection.domain.fingerprint.ErrorFingerprintCalculator;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;
import com.sherlock.logdetective.ingestion.adapter.in.FileSourceProperties;
import com.sherlock.logdetective.ingestion.application.IngestLogsUseCase;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase;
import com.sherlock.logdetective.ingestion.application.ManageFileSourcesUseCase.SourceRegistryPort;
import com.sherlock.logdetective.ingestion.domain.SafePathValidator;
import com.sherlock.logdetective.ingestion.domain.parse.CompositeLogParser;
import com.sherlock.logdetective.ingestion.domain.parse.LogParser;
import com.sherlock.logdetective.ingestion.domain.redaction.BasicRedactionHook;
import com.sherlock.logdetective.ingestion.domain.redaction.RedactionHook;
import com.sherlock.logdetective.ingestion.port.out.EventRepository;
import com.sherlock.logdetective.ingestion.port.out.SourceCheckpointStore;
import com.sherlock.logdetective.investigation.adapter.out.NoOpAiInvestigator;
import com.sherlock.logdetective.investigation.application.InvestigateIncidentUseCase;
import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase;
import com.sherlock.logdetective.investigation.port.out.AiInvestigator;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({ FileSourceProperties.class, DetectionProperties.class })
public class LogDetectiveConfig {

	@Bean
	SafePathValidator safePathValidator(FileSourceProperties properties) {
		String base = properties.getAllowlistBaseDir();
		if (base == null || base.isBlank()) {
			base = "./logs";
		}
		return new SafePathValidator(Path.of(base));
	}

	@Bean
	AiInvestigator aiInvestigator() {
		return new NoOpAiInvestigator();
	}

	@Bean
	InvestigateIncidentUseCase investigateIncidentUseCase(AiInvestigator aiInvestigator) {
		return new InvestigateIncidentUseCase(aiInvestigator);
	}

	@Bean
	RequestInvestigationUseCase requestInvestigationUseCase(
			IncidentRepository incidentRepository,
			EvidenceRepository evidenceRepository,
			InvestigateIncidentUseCase investigateIncidentUseCase,
			AiInvestigator aiInvestigator
	) {
		return new RequestInvestigationUseCase(
				incidentRepository,
				evidenceRepository,
				investigateIncidentUseCase,
				aiInvestigator
		);
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
	DetectionRulesConfig detectionRulesConfig(DetectionProperties detectionProperties) {
		return detectionProperties.toRulesConfig();
	}

	@Bean
	CorrelateEvidenceUseCase correlateEvidenceUseCase(DetectionRulesConfig detectionRulesConfig) {
		return new CorrelateEvidenceUseCase(detectionRulesConfig.correlation());
	}

	@Bean
	QueryIncidentsUseCase queryIncidentsUseCase(IncidentRepository incidentRepository) {
		return new QueryIncidentsUseCase(incidentRepository);
	}

	@Bean
	QueryEvidenceUseCase queryEvidenceUseCase(
			EvidenceRepository evidenceRepository,
			IncidentRepository incidentRepository
	) {
		return new QueryEvidenceUseCase(evidenceRepository, incidentRepository);
	}

	@Bean
	EvaluateDetectionsUseCase evaluateDetectionsUseCase(
			DetectionRulesConfig detectionRulesConfig,
			EventRepository eventRepository,
			RecordErrorObservationUseCase recordErrorObservationUseCase,
			EvidenceRepository evidenceRepository,
			CorrelateEvidenceUseCase correlateEvidenceUseCase,
			ErrorFingerprintCalculator errorFingerprintCalculator
	) {
		return new EvaluateDetectionsUseCase(
				detectionRulesConfig,
				eventRepository,
				recordErrorObservationUseCase,
				evidenceRepository,
				correlateEvidenceUseCase,
				errorFingerprintCalculator
		);
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
			RecordErrorObservationUseCase recordErrorObservationUseCase,
			EvaluateDetectionsUseCase evaluateDetectionsUseCase
	) {
		return new IngestLogsUseCase(
				eventRepository,
				checkpointStore,
				logParser,
				redactionHook,
				recordErrorObservationUseCase,
				evaluateDetectionsUseCase
		);
	}

	@Bean
	ManageFileSourcesUseCase manageFileSourcesUseCase(
			SafePathValidator safePathValidator,
			SourceRegistryPort sourceRegistryPort,
			IngestLogsUseCase ingestLogsUseCase,
			FileSourceProperties properties
	) {
		return new ManageFileSourcesUseCase(
				safePathValidator,
				sourceRegistryPort,
				ingestLogsUseCase,
				properties.getBatchSize()
		);
	}

	@Bean
	CliFacade cliFacade(
			ManageFileSourcesUseCase manageFileSourcesUseCase,
			QueryIncidentsUseCase queryIncidentsUseCase,
			QueryEvidenceUseCase queryEvidenceUseCase,
			RequestInvestigationUseCase requestInvestigationUseCase
	) {
		return new CliFacade(
				manageFileSourcesUseCase,
				queryIncidentsUseCase,
				queryEvidenceUseCase,
				requestInvestigationUseCase
		);
	}
}
