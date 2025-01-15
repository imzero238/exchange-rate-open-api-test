package com.nayoung.exchangerateopenapitest.domain.exchangerate.naver;

import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import com.nayoung.exchangerateopenapitest.domain.exchangerate.naver.dto.ExchangeRateNaverResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
	name = "naver-exchange-rate-service",
	url = "${exchange-rate.naver.host}"
)
public interface ExchangeRateNaverClient {

	@GetMapping("${exchange-rate.naver.url}}")
	ExchangeRateNaverResponseDto getExchangeRate(
		@RequestParam("key") String key,
		@RequestParam("pkid") String pkid,
		@RequestParam("q") String q,
		@RequestParam("where") String where,
		@RequestParam("u1") String u1,
		@RequestParam("u6") String u6,
		@RequestParam("u7") String u7,
		@RequestParam("u3") Currency fromCurrency,
		@RequestParam("u4") Currency toCurrency,
		@RequestParam("u8") String u8,
		@RequestParam("u2") String u2
	);
}
