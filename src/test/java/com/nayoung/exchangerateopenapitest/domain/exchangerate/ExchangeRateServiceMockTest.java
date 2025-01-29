package com.nayoung.exchangerateopenapitest.domain.exchangerate;

import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import com.nayoung.exchangerateopenapitest.domain.exchangerate.naver.ExchangeRateNaverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceMockTest {

	@InjectMocks
	ExchangeRateService exchangeRateService;

	@Mock
	ExchangeRateNaverService exchangeRateNaverService;

	@Test
	void open_api_호출_제한_테스트 () throws InterruptedException {
		Mockito.when(exchangeRateNaverService.getExchangeRate(any(), any()))
			.thenReturn(new BigDecimal(1));

		int threadCount = 20;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch countDownLatch = new CountDownLatch(threadCount);

		for(int i = 0; i < threadCount; i++) {
			executorService.submit(() -> {
				try {
					exchangeRateService.getLatestExchangeRate(Currency.USD, Currency.KRW);
				} finally {
					countDownLatch.countDown();
				}
			});
		}
		countDownLatch.await();
		executorService.shutdown();

		verify(exchangeRateNaverService, times(1))
			.getExchangeRate(any(), any());
	}
}