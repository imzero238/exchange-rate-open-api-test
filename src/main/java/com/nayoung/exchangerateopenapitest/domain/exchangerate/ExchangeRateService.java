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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

	private final ExchangeRateNaverService naverService;
	private final ExchangeRateMananaService mananaService;
	private final ExchangeRateGoogleFinanceScraper googleFinanceScraper;

	private static final Map<Currency, ReentrantLock> currencyLocks = new ConcurrentHashMap<>();
	private static final Map<Currency, CompletableFuture<BigDecimal>> currencyFutures = new ConcurrentHashMap<>();
	private static final Map<Currency, ExchangeRateStatus> exchangeRateResults = new ConcurrentHashMap<>();

	private static final long TRY_LOCK_TIMEOUT = 1;
	private static final long CACHE_EXPIRY_TIME = 2000;
	private static final long OPEN_API_TIMEOUT = 2000;
	private static final long FUTURE_TIMEOUT = 2000;

	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		.withZone(ZoneId.systemDefault());

	public BigDecimal getLatestExchangeRate(Currency fromCurrency, Currency toCurrency) {
		if(isAvailableExchangeRate(fromCurrency, toCurrency)) {
			return exchangeRateResults.get(fromCurrency).exchangeRate;
		}

		ReentrantLock lock = currencyLocks.computeIfAbsent(fromCurrency, k -> new ReentrantLock());
		CompletableFuture<BigDecimal> future = currencyFutures.computeIfAbsent(fromCurrency, k -> new CompletableFuture<>());
		try {
			if (lock.tryLock(TRY_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
				return fetchPrimaryExchangeRate(fromCurrency, toCurrency, lock, future);
			} else {
				return monitorExchangeRateUpdate(fromCurrency, toCurrency, lock, future);
			}
		} catch (InterruptedException e) {
			log.error("{} ReentrantLock 획득 중 스레드 인터럽트 발생", fromCurrency, e);
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		} catch (Exception e) {
			log.error("{} 환율 조회 중 예상치 못한 예외 발생", fromCurrency, e);
			throw new RuntimeException(e);
		}

	}

	private BigDecimal fetchPrimaryExchangeRate(Currency fromCurrency, Currency toCurrency, ReentrantLock lock, CompletableFuture<BigDecimal> future) {
		try {
			BigDecimal latestExchangeRate = CompletableFuture
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

			future.complete(latestExchangeRate);
			return latestExchangeRate;
		} finally {
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

	private BigDecimal monitorExchangeRateUpdate(Currency fromCurrency, Currency toCurrency, ReentrantLock lock, CompletableFuture<BigDecimal> future) throws InterruptedException {
		// double checking
		if(isAvailableExchangeRate(fromCurrency, toCurrency)) {
			return exchangeRateResults.get(fromCurrency).exchangeRate;
		}

		log.info("다른 스레드에 의해 {} 단위가 업데이트 중입니다.", fromCurrency);

		return future.orTimeout(FUTURE_TIMEOUT, TimeUnit.MILLISECONDS)
			.thenApply(result -> {
				log.info("업데이트된 환율({}, {}) 사용", fromCurrency, result);
				return result;
			})
			.exceptionally(ex -> {
				log.warn("{} 환율 업데이트 대기 시간 초과, 직접 환율 Open API 시도", fromCurrency);
				try {
					// 생산자 스레드의 알 수 없는 오류로 직접 Open API 직접 호출, 소비자 -> 생산자 역할 전환
					if (lock.tryLock(TRY_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
						return fetchPrimaryExchangeRate(fromCurrency, toCurrency, lock, future);
					} else {  // ReentrantLock 획득 실패, 마지막 대기 시도
						return future.get(FUTURE_TIMEOUT, TimeUnit.MILLISECONDS);
					}
				} catch (TimeoutException e) {
					log.error("환율 데이터 대기 시간 초과", e);
					throw new RuntimeException(e);
				} catch (Exception e) {
					log.error("환율 조회 중 예상치 못한 예외 발생", e);
					throw new RuntimeException(e);
				} finally {
					if (lock.isHeldByCurrentThread()) {
						lock.unlock();
					}
				}
			}).join();
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
