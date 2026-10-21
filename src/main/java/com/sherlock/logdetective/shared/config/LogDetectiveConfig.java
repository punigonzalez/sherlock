package com.sherlock.logdetective.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
