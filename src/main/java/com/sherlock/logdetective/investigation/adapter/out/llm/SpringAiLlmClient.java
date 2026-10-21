package com.sherlock.logdetective.investigation.adapter.out.llm;

import java.util.Objects;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import com.sherlock.logdetective.investigation.port.out.LlmClient;

/**
 * Thin Spring AI ChatModel adapter (Boot 4.1 / Spring AI 2.x compatible).
 */
public final class SpringAiLlmClient implements LlmClient {

	private final ChatModel chatModel;
	private final String providerName;

	public SpringAiLlmClient(ChatModel chatModel, String providerName) {
		this.chatModel = Objects.requireNonNull(chatModel);
		this.providerName = providerName == null || providerName.isBlank() ? "spring-ai" : providerName;
	}

	@Override
	public boolean available() {
		return chatModel != null;
	}

	@Override
	public String providerName() {
		return providerName;
	}

	@Override
	public String complete(String systemPrompt, String userPrompt) {
		var response = chatModel.call(new Prompt(java.util.List.of(
				new SystemMessage(systemPrompt),
				new UserMessage(userPrompt)
		)));
		if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
			return "";
		}
		return response.getResult().getOutput().getText();
	}
}
