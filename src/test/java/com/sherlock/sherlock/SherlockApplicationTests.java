package com.sherlock.sherlock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.sherlock.logdetective.investigation.port.out.AiInvestigator;

@SpringBootTest
@Testcontainers
class SherlockApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	private AiInvestigator aiInvestigator;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
		assertThat(aiInvestigator).isNotNull();
	}

	@Test
	void flywayMigrationsAppliedThroughPhase2() {
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
				Integer.class
		)).isGreaterThanOrEqualTo(2);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT schema_version FROM app_schema_meta WHERE id = 1",
				String.class
		)).isEqualTo("phase-2-incidents");
	}
}
