package br.com.devtasker.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DevtaskerApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevtaskerApiApplication.class, args);
	}

}
