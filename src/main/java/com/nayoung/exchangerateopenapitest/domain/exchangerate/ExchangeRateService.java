package com.nayoung.exchangerateopenapitest.domain.exchangerate;

import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import com.nayoung.exchangerateopenapitest.domain.exchangerate.google.ExchangeRateGoogleFinanceScraper;
import com.nayoung.exchangerateopenapitest.domain.exchangerate.manana.ExchangeRateMananaService;
import com.nayoung.exchangerateopenapitest.domain.exchangerate.naver.ExchangeRateNaverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

	private final ExchangeRateNaverService naverService;
	private final ExchangeRateMananaService mananaService;
	private final ExchangeRateGoogleFinanceScraper googleFinanceScraper;

	private final ConcurrentHashMap<Currency, ReentrantLock> currencyLocks = new ConcurrentHashMap<>();
	private final Map<Currency, ExchangeRateStatus> exchangeRateResults = new ConcurrentHashMap<>();
	private final long CACHE_EXPIRY_TIME = 2000;

	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		.withZone(ZoneId.systemDefault());

	public void updateExchangeRate(Currency fromCurrency, Currency toCurrency) {
		if(isAvailableExchangeRate(fromCurrency, toCurrency)) {
			return;
		}

		ReentrantLock lock = currencyLocks.computeIfAbsent(fromCurrency, k -> new ReentrantLock());
		if(lock.tryLock()) {
			try {
				// double checking
				if (isAvailableExchangeRate(fromCurrency, toCurrency)) {
					return;
				}

				CompletableFuture
					.supplyAsync(() -> naverService.getExchangeRate(fromCurrency, toCurrency))
					.orTimeout(CACHE_EXPIRY_TIME, TimeUnit.MILLISECONDS)
					.thenApply(result -> new BigDecimal(result.toString())
						.setScale(2, RoundingMode.CEILING))
					.thenApply(exchangeRate -> {
						updateExchangeRateStatus(fromCurrency, exchangeRate);

						log.info("[Main(Naver)] {} 환율 {} 업데이트, {}",
							fromCurrency,
							exchangeRate,
							formatter.format(Instant.ofEpochMilli(exchangeRateResults.get(fromCurrency).lastCachedTime)));
						return exchangeRate;
					})
					.exceptionally(ex -> {
						log.error("Naver API failed or timed out, calling Manana and Google... {}", ex.getMessage());
						fallbackUpdate(fromCurrency, toCurrency);
						return null;
					}).join();
			} finally {
				lock.unlock();
			}
		} else {
			log.info("다른 스레드에 의해 {} 단위가 업데이트 중입니다.", fromCurrency);
		}
	}

	private void fallbackUpdate(Currency fromCurrency, Currency toCurrency) {
		CompletableFuture
			.anyOf(
				CompletableFuture
					.supplyAsync(() -> mananaService.getExchangeRate(fromCurrency, toCurrency))
					.orTimeout(CACHE_EXPIRY_TIME, TimeUnit.MILLISECONDS),

				CompletableFuture
					.supplyAsync(() -> googleFinanceScraper.getExchangeRate(fromCurrency, toCurrency))
					.orTimeout(CACHE_EXPIRY_TIME, TimeUnit.MILLISECONDS))

			.thenApply(result -> new BigDecimal(result.toString())
				.setScale(2, RoundingMode.CEILING))
			.thenApply(exchangeRate -> {
				updateExchangeRateStatus(fromCurrency, exchangeRate);

				log.info("[Fallback] {} 환율 {} 업데이트, {}",
					fromCurrency,
					exchangeRateResults.get(fromCurrency).exchangeRate,
					formatter.format(Instant.ofEpochMilli(exchangeRateResults.get(fromCurrency).lastCachedTime)));

				return exchangeRate;
			})
			.exceptionally(ex -> {
				log.error(ex.getMessage());
				throw new RuntimeException("Unable to fetch exchange rate", ex);
			})
			.join();
	}

	private void updateExchangeRateStatus(Currency fromCurrency, BigDecimal exchangeRate) {
		ExchangeRateStatus status = exchangeRateResults.computeIfAbsent(fromCurrency, k -> new ExchangeRateStatus());
		status.exchangeRate = exchangeRate;
		status.lastCachedTime = System.currentTimeMillis();
	}

	public boolean isAvailableExchangeRate(Currency fromCurrency, Currency toCurrency) {
		return exchangeRateResults.containsKey(fromCurrency)
			&& System.currentTimeMillis() - exchangeRateResults.get(fromCurrency).lastCachedTime < CACHE_EXPIRY_TIME;
	}

	public BigDecimal getExchangeRate(Currency fromCurrency, Currency toCurrency) {
		return exchangeRateResults.get(fromCurrency).exchangeRate;
	}

	class ExchangeRateStatus {
		BigDecimal exchangeRate;
		Long lastCachedTime;

		public ExchangeRateStatus() {}
	}
}
