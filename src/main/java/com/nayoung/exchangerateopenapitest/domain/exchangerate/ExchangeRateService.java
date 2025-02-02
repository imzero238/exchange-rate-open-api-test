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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

	private final ExchangeRateNaverService naverService;
	private final ExchangeRateMananaService mananaService;
	private final ExchangeRateGoogleFinanceScraper googleFinanceScraper;

	private static final Map<Currency, ReentrantLock> currencyLocks = new ConcurrentHashMap<>();
	private static final Map<Currency, Condition> currencyConditions = new ConcurrentHashMap<>();
	private static final Map<Currency, ExchangeRateStatus> exchangeRateResults = new ConcurrentHashMap<>();

	private static final long TRY_LOCK_TIMEOUT = 1;
	private static final long CACHE_EXPIRY_TIME = 2000;
	private static final long OPEN_API_TIMEOUT = 2000;
	private static final long CONDITION_WAIT_TIMEOUT = 3000;

	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		.withZone(ZoneId.systemDefault());

	public BigDecimal getLatestExchangeRate(Currency fromCurrency, Currency toCurrency) {
		if(isAvailableExchangeRate(fromCurrency, toCurrency)) {
			return exchangeRateResults.get(fromCurrency).exchangeRate;
		}

		ReentrantLock lock = currencyLocks.computeIfAbsent(fromCurrency, k -> new ReentrantLock());
		Condition condition = currencyConditions.computeIfAbsent(fromCurrency, k -> lock.newCondition());
		try {
			if (lock.tryLock(TRY_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
				return fetchPrimaryExchangeRate(fromCurrency, toCurrency, lock, condition);
			} else {
				return monitorExchangeRateUpdate(fromCurrency, toCurrency, lock, condition);
			}
		} catch (InterruptedException e) {
			log.error("{} ReentrantLock 획득 중 스레드 인터럽트 발생: {}", fromCurrency, e.getMessage());
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}

	private BigDecimal fetchPrimaryExchangeRate(Currency fromCurrency, Currency toCurrency, ReentrantLock lock, Condition condition) {
		try {
			// double checking
			if(isAvailableExchangeRate(fromCurrency, toCurrency)) {
				return exchangeRateResults.get(fromCurrency).exchangeRate;
			}

			return CompletableFuture
				.supplyAsync(() -> naverService.getExchangeRate(fromCurrency, toCurrency))
				.orTimeout(OPEN_API_TIMEOUT, TimeUnit.MILLISECONDS)
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
					return fetchFallbackExchangeRate(fromCurrency, toCurrency);
				}).join();
		} finally {
			synchronized (condition) {
				condition.notifyAll();
			}
			lock.unlock();
		}
	}

	private BigDecimal fetchFallbackExchangeRate(Currency fromCurrency, Currency toCurrency) {
		return CompletableFuture
			.anyOf(
				CompletableFuture
					.supplyAsync(() -> mananaService.getExchangeRate(fromCurrency, toCurrency))
					.orTimeout(OPEN_API_TIMEOUT, TimeUnit.MILLISECONDS),

				CompletableFuture
					.supplyAsync(() -> googleFinanceScraper.getExchangeRate(fromCurrency, toCurrency))
					.orTimeout(OPEN_API_TIMEOUT, TimeUnit.MILLISECONDS)
			)
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

	private BigDecimal monitorExchangeRateUpdate(Currency fromCurrency, Currency toCurrency, ReentrantLock lock, Condition condition) throws InterruptedException {
		try {
			// double checking
			if(isAvailableExchangeRate(fromCurrency, toCurrency)) {
				return exchangeRateResults.get(fromCurrency).exchangeRate;
			}

			log.info("다른 스레드에 의해 {} 단위가 업데이트 중입니다.", fromCurrency);

			while (!isAvailableExchangeRate(fromCurrency, toCurrency)) {
				if(lock.tryLock(TRY_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
					return fetchPrimaryExchangeRate(fromCurrency, toCurrency, lock, condition);
				}
				else {
					synchronized (condition) {
						condition.wait(CONDITION_WAIT_TIMEOUT);
					}
				}
			}
			return exchangeRateResults.get(fromCurrency).exchangeRate;
		} finally {
			if(lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	private void updateExchangeRateStatus(Currency fromCurrency, BigDecimal exchangeRate) {
		ExchangeRateStatus status = exchangeRateResults.computeIfAbsent(fromCurrency, k -> new ExchangeRateStatus());
		status.exchangeRate = exchangeRate;
		status.lastCachedTime = System.currentTimeMillis();
	}

	private boolean isAvailableExchangeRate(Currency fromCurrency, Currency toCurrency) {
		return exchangeRateResults.containsKey(fromCurrency)
			&& System.currentTimeMillis() - exchangeRateResults.get(fromCurrency).lastCachedTime < CACHE_EXPIRY_TIME;
	}

	static class ExchangeRateStatus {
		BigDecimal exchangeRate;
		Long lastCachedTime;

		public ExchangeRateStatus() {}
	}
}
