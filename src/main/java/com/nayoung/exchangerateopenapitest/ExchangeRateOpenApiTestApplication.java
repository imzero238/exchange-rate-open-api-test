package com.nayoung.exchangerateopenapitest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ExchangeRateOpenApiTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExchangeRateOpenApiTestApplication.class, args);
	}

}
