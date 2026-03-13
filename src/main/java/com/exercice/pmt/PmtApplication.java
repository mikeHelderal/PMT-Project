package com.exercice.pmt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PmtApplication {

	public static void main(String[] args) {
		SpringApplication.run(PmtApplication.class, args);
	}

}
