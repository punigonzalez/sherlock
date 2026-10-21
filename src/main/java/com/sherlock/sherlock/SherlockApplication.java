package com.sherlock.sherlock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.sherlock")
public class SherlockApplication {

	public static void main(String[] args) {
		SpringApplication.run(SherlockApplication.class, args);
	}

}
