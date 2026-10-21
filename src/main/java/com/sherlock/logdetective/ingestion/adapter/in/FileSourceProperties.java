package com.sherlock.logdetective.ingestion.adapter.in;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logdetective.ingestion")
public class FileSourceProperties {

	private boolean enabled = false;
	private int batchSize = 100;
	private long pollIntervalMs = 2000L;
	private List<SourceConfig> sources = new ArrayList<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public long getPollIntervalMs() {
		return pollIntervalMs;
	}

	public void setPollIntervalMs(long pollIntervalMs) {
		this.pollIntervalMs = pollIntervalMs;
	}

	public List<SourceConfig> getSources() {
		return sources;
	}

	public void setSources(List<SourceConfig> sources) {
		this.sources = sources;
	}

	public static class SourceConfig {
		private String id;
		private String type = "FILE";
		private String path;
		private String encoding = StandardCharsets.UTF_8.name();
		private boolean enabled = true;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getEncoding() {
			return encoding;
		}

		public void setEncoding(String encoding) {
			this.encoding = encoding;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Charset charset() {
			return Charset.forName(encoding == null || encoding.isBlank()
					? StandardCharsets.UTF_8.name()
					: encoding);
		}
	}
}
