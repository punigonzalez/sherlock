package com.sherlock.logdetective.ingestion.adapter.in;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logdetective.gcp-logging")
public class GcpLoggingProperties {

	private boolean enabled = false;
	private String sourceId = "gcp-logging";
	private String projectId = "";
	private String filter = "severity>=ERROR";
	private int pageSize = 50;
	private long pollIntervalMs = 10_000L;
	/** Bounded historical window on first pull (ISO-8601 duration, e.g. PT1H). */
	private Duration historicalLookback = Duration.ofHours(1);
	private int maxPagesPerPoll = 3;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public long getPollIntervalMs() {
		return pollIntervalMs;
	}

	public void setPollIntervalMs(long pollIntervalMs) {
		this.pollIntervalMs = pollIntervalMs;
	}

	public Duration getHistoricalLookback() {
		return historicalLookback;
	}

	public void setHistoricalLookback(Duration historicalLookback) {
		this.historicalLookback = historicalLookback;
	}

	public int getMaxPagesPerPoll() {
		return maxPagesPerPoll;
	}

	public void setMaxPagesPerPoll(int maxPagesPerPoll) {
		this.maxPagesPerPoll = maxPagesPerPoll;
	}
}
