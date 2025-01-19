package com.nayoung.exchangerateopenapitest.domain.exchangerate.manana;

import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import com.nayoung.exchangerateopenapitest.domain.exchangerate.manana.dto.ExchangeRateMananaResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
	name = "manana-exchange-rate-client",
	url = "https://api.manana.kr/exchange"
)
public interface ExchangeRateMananaClient {

	@GetMapping("/rate.json")
	List<ExchangeRateMananaResponseDto> getExchangeRate(
		@RequestParam("base") Currency base,
		@RequestParam("code") Currency code
	);
}
