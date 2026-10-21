package com.sherlock.logdetective.investigation.adapter.out.llm;

import com.sherlock.logdetective.investigation.port.out.LlmClient;

public final class UnavailableLlmClient implements LlmClient {

	private final String providerName;

	public UnavailableLlmClient(String providerName) {
		this.providerName = providerName == null ? "none" : providerName;
	}

	@Override
	public boolean available() {
		return false;
	}

	@Override
	public String providerName() {
		return providerName;
	}

	@Override
	public String complete(String systemPrompt, String userPrompt) {
		throw new IllegalStateException("LLM client unavailable");
	}
}
