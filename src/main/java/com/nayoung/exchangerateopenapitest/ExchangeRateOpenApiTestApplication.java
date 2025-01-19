package com.nayoung.exchangerateopenapitest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableFeignClients(basePackages = "com.nayoung.exchangerateopenapitest.domain.exchangerate")
public class ExchangeRateOpenApiTestApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExchangeRateOpenApiTestApplication.class, args);
	}

}
