package com.nayoung.exchangerateopenapitest.domain.exchangerate.naver;

import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ExchangeRateNaverServiceTest {

	@Autowired
	ExchangeRateNaverService exchangeRateNaverService;

	@Test
	void USD_KRW_test() {
		BigDecimal exchangeRate = exchangeRateNaverService.getExchangeRate(Currency.USD, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());
	}

	@Test
	void JPY_KRW_test() {
		BigDecimal exchangeRate = exchangeRateNaverService.getExchangeRate(Currency.JPY, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());
	}

	@Test
	void EUR_KRW_test() {
		BigDecimal exchangeRate = exchangeRateNaverService.getExchangeRate(Currency.EUR, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());
	}
}