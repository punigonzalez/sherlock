package com.sherlock.logdetective.shared.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;

/**
 * Optional OpenAI ChatModel via Spring AI 2.x. Only loads when provider=openai and API key is set.
 * Build/tests never require a real key.
 */
@Configuration
@ConditionalOnProperty(prefix = "logdetective.investigation", name = "provider", havingValue = "openai")
public class OptionalOpenAiConfig {

	@Bean
	@ConditionalOnExpression("!'${spring.ai.openai.api-key:}'.blank")
	ChatModel openAiChatModel(
			@Value("${spring.ai.openai.api-key}") String apiKey,
			@Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String model
	) {
		ClientOptions options = ClientOptions.builder().apiKey(apiKey).build();
		OpenAIClient client = new OpenAIClientImpl(options);
		return OpenAiChatModel.builder()
				.openAiClient(client)
				.options(OpenAiChatOptions.builder().model(model).build())
				.build();
	}
}
