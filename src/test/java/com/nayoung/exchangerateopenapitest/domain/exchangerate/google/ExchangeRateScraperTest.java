package com.nayoung.exchangerateopenapitest.domain.exchangerate.google;

import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ExchangeRateScraperTest {

	@Autowired
	ExchangeRateScraper exchangeRateScraper;

	@Test
	void USD_KRW_test() {
		String fromCurrency = String.valueOf(Currency.USD);
		String toCurrency = String.valueOf(Currency.KRW);

		BigDecimal exchangeRate = exchangeRateScraper.getExchangeRate(fromCurrency, toCurrency);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());
	}

	@Test
	void JPY_KRW_test() {
		String fromCurrency = String.valueOf(Currency.JPY);
		String toCurrency = String.valueOf(Currency.KRW);

		BigDecimal exchangeRate = exchangeRateScraper.getExchangeRate(fromCurrency, toCurrency);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());
	}

	@Test
	void EUR_KRW_test() {
		String fromCurrency = String.valueOf(Currency.EUR);
		String toCurrency = String.valueOf(Currency.KRW);

		BigDecimal exchangeRate = exchangeRateScraper.getExchangeRate(fromCurrency, toCurrency);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());
	}

	@Test
	void invalid_currency_test() {
		String fromCurrency = "USDD";
		String toCurrency = String.valueOf(Currency.KRW);

		Assertions.assertThatThrownBy(() -> exchangeRateScraper.getExchangeRate(fromCurrency, toCurrency))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("No exchange rate information found for: USDD to KRW");
	}
}