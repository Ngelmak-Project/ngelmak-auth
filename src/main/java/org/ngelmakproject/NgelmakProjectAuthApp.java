package org.ngelmakproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class NgelmakProjectAuthApp {

	public static void main(String[] args) {
		SpringApplication.run(NgelmakProjectAuthApp.class, args);
	}

}
