package com.sherlock.logdetective.ingestion.adapter.out.gcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.sherlock.logdetective.ingestion.domain.LogSourceId;
import com.sherlock.logdetective.ingestion.port.out.LogSource;

/**
 * Opt-in live GCP test. Disabled by default — set LOGDETECTIVE_GCP_IT=true and ADC + GCP_PROJECT_ID.
 */
@EnabledIfEnvironmentVariable(named = "LOGDETECTIVE_GCP_IT", matches = "true")
class GcpLoggingLiveIntegrationTest {

	@Test
	void validatesAndFetchesWithAdc() {
		String projectId = System.getenv("GCP_PROJECT_ID");
		assumeTrue(projectId != null && !projectId.isBlank(), "GCP_PROJECT_ID required");

		try (GoogleCloudLoggingAdapter adapter = new GoogleCloudLoggingAdapter(projectId)) {
			GcpLogSource source = new GcpLogSource(
					new LogSourceId("gcp-it"),
					adapter,
					System.getenv().getOrDefault("LOGDETECTIVE_GCP_FILTER", "severity>=ERROR"),
					Duration.ofMinutes(15),
					10,
					1
			);
			source.validate();
			source.start();
			var batch = source.fetch(LogSource.FetchCursor.start(), 10);
			assertThat(batch).isNotNull();
			source.stop();
		}
	}
}
