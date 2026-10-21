package com.sherlock.logdetective.investigation.port.out;

/**
 * Provider-neutral chat completion. Optional; Fake/NoOp investigators do not require it.
 */
public interface LlmClient {

	boolean available();

	String providerName();

	String complete(String systemPrompt, String userPrompt);
}
