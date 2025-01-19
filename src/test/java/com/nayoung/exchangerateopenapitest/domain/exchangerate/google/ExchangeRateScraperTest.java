package com.nayoung.exchangerateopenapitest.domain.exchangerate.google;

import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ExchangeRateScraperTest {

	@Autowired
	ExchangeRateGoogleFinanceScraper exchangeRateGoogleFinanceScraper;

	@Test
	void USD_KRW_test() {
		BigDecimal exchangeRate = exchangeRateGoogleFinanceScraper.getExchangeRate(Currency.USD, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());
	}

	@Test
	void JPY_KRW_test() {
		BigDecimal exchangeRate = exchangeRateGoogleFinanceScraper.getExchangeRate(Currency.JPY, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());
	}

	@Test
	void EUR_KRW_test() {
		BigDecimal exchangeRate = exchangeRateGoogleFinanceScraper.getExchangeRate(Currency.EUR, Currency.KRW);

		assertNotNull(exchangeRate);
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) > 0);
		assertEquals(2, exchangeRate.scale());
	}

	@Test
	void invalid_currency_test() {
		Assertions.assertThatThrownBy(() -> exchangeRateGoogleFinanceScraper.getExchangeRate(Currency.USDD, Currency.KRW))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("No exchange rate information found for: USDD to KRW");
	}
}