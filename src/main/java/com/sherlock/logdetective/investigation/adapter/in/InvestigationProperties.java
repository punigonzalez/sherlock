package com.sherlock.logdetective.investigation.adapter.in;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logdetective.investigation")
public class InvestigationProperties {

	/**
	 * noop | fake | openai (Spring AI ChatModel when configured).
	 */
	private String provider = "noop";
	private int maxToolCalls = 8;
	private int timeoutSeconds = 30;
	private int maxLogChars = 2000;
	private int toolRowLimit = 20;

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public int getMaxToolCalls() {
		return maxToolCalls;
	}

	public void setMaxToolCalls(int maxToolCalls) {
		this.maxToolCalls = maxToolCalls;
	}

	public int getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public int getMaxLogChars() {
		return maxLogChars;
	}

	public void setMaxLogChars(int maxLogChars) {
		this.maxLogChars = maxLogChars;
	}

	public int getToolRowLimit() {
		return toolRowLimit;
	}

	public void setToolRowLimit(int toolRowLimit) {
		this.toolRowLimit = toolRowLimit;
	}
}
