package com.bridge.plugin.treegraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import sdk.ClassPatcher;

@SpringBootApplication
@EnableScheduling
public class Application {

	public static void main(String[] args) {
		ClassPatcher.changeCode();
		SpringApplication.run(Application.class, args);
	}

}
