package com.sherlock.logdetective.shared.config;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
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
import com.sherlock.logdetective.ingestion.adapter.in.GcpLoggingProperties;
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
import com.sherlock.logdetective.investigation.adapter.in.InvestigationProperties;
import com.sherlock.logdetective.investigation.adapter.out.FakeAiInvestigator;
import com.sherlock.logdetective.investigation.adapter.out.NoOpAiInvestigator;
import com.sherlock.logdetective.investigation.adapter.out.ToolBoundedAiInvestigator;
import com.sherlock.logdetective.investigation.adapter.out.llm.SpringAiLlmClient;
import com.sherlock.logdetective.investigation.adapter.out.llm.UnavailableLlmClient;
import com.sherlock.logdetective.investigation.adapter.out.tools.RepositoryInvestigationTools;
import com.sherlock.logdetective.investigation.application.InvestigateIncidentUseCase;
import com.sherlock.logdetective.investigation.application.RequestInvestigationUseCase;
import com.sherlock.logdetective.investigation.domain.PromptSanitizer;
import com.sherlock.logdetective.investigation.port.out.AiInvestigator;
import com.sherlock.logdetective.investigation.port.out.InvestigationReportRepository;
import com.sherlock.logdetective.investigation.port.out.InvestigationTools;
import com.sherlock.logdetective.investigation.port.out.LlmClient;

import org.springframework.ai.chat.model.ChatModel;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
		FileSourceProperties.class,
		GcpLoggingProperties.class,
		DetectionProperties.class,
		InvestigationProperties.class
})
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
	PromptSanitizer promptSanitizer(InvestigationProperties properties) {
		return new PromptSanitizer(properties.getMaxLogChars());
	}

	@Bean
	InvestigationTools investigationTools(
			IncidentRepository incidentRepository,
			EvidenceRepository evidenceRepository,
			EventRepository eventRepository,
			RedactionHook redactionHook,
			PromptSanitizer promptSanitizer,
			InvestigationProperties properties
	) {
		return new RepositoryInvestigationTools(
				incidentRepository,
				evidenceRepository,
				eventRepository,
				redactionHook,
				promptSanitizer,
				properties.getToolRowLimit()
		);
	}

	@Bean
	LlmClient llmClient(ObjectProvider<ChatModel> chatModel, InvestigationProperties properties) {
		ChatModel model = chatModel.getIfAvailable();
		if (model != null && "openai".equalsIgnoreCase(properties.getProvider())) {
			return new SpringAiLlmClient(model, "openai");
		}
		return new UnavailableLlmClient(properties.getProvider());
	}

	@Bean
	AiInvestigator aiInvestigator(
			InvestigationProperties properties,
			InvestigationTools investigationTools,
			LlmClient llmClient
	) {
		String provider = properties.getProvider() == null ? "noop" : properties.getProvider().trim().toLowerCase();
		return switch (provider) {
			case "fake" -> new FakeAiInvestigator(investigationTools, properties.getMaxToolCalls());
			case "openai" -> new ToolBoundedAiInvestigator(
					investigationTools,
					llmClient,
					properties.getMaxToolCalls(),
					Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds()))
			);
			default -> new NoOpAiInvestigator();
		};
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
			InvestigationReportRepository reportRepository
	) {
		return new RequestInvestigationUseCase(
				incidentRepository,
				evidenceRepository,
				investigateIncidentUseCase,
				reportRepository
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
