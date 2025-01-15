package com.nayoung.exchangerateopenapitest.domain.exchangerate.naver.dto;

import java.util.List;

public record ExchangeRateNaverResponseDto (
	int pkid,
	int count,
	List<Country> country,
	String calculatorMessage
) {
	public record Country (
		String value,
		String subValue,
		String currencyUnit
	) {}
}
