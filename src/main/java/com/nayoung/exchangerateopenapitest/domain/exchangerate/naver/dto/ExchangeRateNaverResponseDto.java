package com.nayoung.exchangerateopenapitest.domain.exchangerate.naver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateNaverResponseDto (
	@JsonProperty("pkid") int pkid,
	@JsonProperty("count") int count,
	@JsonProperty("country") List<Country> country,
	@JsonProperty("calculatorMessage") String calculatorMessage
) {
	public record Country (
		@JsonProperty("value") String value,
		@JsonProperty("subValue") String subValue,
		@JsonProperty("currencyUnit") String currencyUnit
	) {}
}
