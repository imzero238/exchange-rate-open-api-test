package com.nayoung.exchangerateopenapitest.domain.exchangerate.manana.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateMananaResponseDto (
	@JsonProperty("data") String data,
	@JsonProperty("name") String name,
	@JsonProperty("rate") BigDecimal rate,
	@JsonProperty("timestamp") long timestamp
) {
}
