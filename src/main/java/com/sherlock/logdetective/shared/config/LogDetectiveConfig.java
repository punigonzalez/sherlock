package com.sherlock.logdetective.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sherlock.logdetective.detection.application.RecordErrorObservationUseCase;
import com.sherlock.logdetective.detection.domain.fingerprint.ErrorFingerprintCalculator;
import com.sherlock.logdetective.detection.port.out.IncidentRepository;
import com.sherlock.logdetective.investigation.adapter.out.NoOpAiInvestigator;
import com.sherlock.logdetective.investigation.application.InvestigateIncidentUseCase;
import com.sherlock.logdetective.investigation.port.out.AiInvestigator;

@Configuration
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
}
