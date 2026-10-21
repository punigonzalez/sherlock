package com.sherlock.sherlock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.sherlock")
@EntityScan(basePackages = "com.sherlock")
@EnableJpaRepositories(basePackages = "com.sherlock")
public class SherlockApplication {

	public static void main(String[] args) {
		SpringApplication.run(SherlockApplication.class, args);
	}

}
