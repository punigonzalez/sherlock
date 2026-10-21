package com.sherlock.logdetective.cli;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Activates when {@code --cli=<command>} is present; runs one command and exits the JVM.
 */
@Component
@Order(0)
public class CliApplicationRunner implements ApplicationRunner {

	private final CliFacade cliFacade;
	private final ConfigurableApplicationContext context;

	public CliApplicationRunner(CliFacade cliFacade, ConfigurableApplicationContext context) {
		this.cliFacade = cliFacade;
		this.context = context;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!args.containsOption("cli")) {
			return;
		}
		int code = cliFacade.execute(args, System.out, System.err);
		SpringApplication.exit(context, () -> code);
		System.exit(code);
	}
}
