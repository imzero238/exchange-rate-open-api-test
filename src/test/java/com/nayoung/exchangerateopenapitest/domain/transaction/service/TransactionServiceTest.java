package com.nayoung.exchangerateopenapitest.domain.transaction.service;

import com.nayoung.exchangerateopenapitest.api.dto.account.AccountDto;
import com.nayoung.exchangerateopenapitest.api.dto.account.AccountRegisterRequestDto;
import com.nayoung.exchangerateopenapitest.api.dto.transaction.TransactionRequestDto;
import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import com.nayoung.exchangerateopenapitest.domain.account.service.AccountService;
import com.nayoung.exchangerateopenapitest.domain.exchangerate.naver.ExchangeRateNaverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

	@Autowired
	TransactionService transactionService;

	@Autowired
	AccountService accountService;

	@MockitoBean
	ExchangeRateNaverService exchangeRateNaverService;

	@Test
	void currency_only_reentrant_lock_test () throws InterruptedException {
		// 테스트용 계정 생성
		AccountDto krwAccount = registerAccount(Currency.KRW);
		AccountDto usdAccount = registerAccount(Currency.USD);
		AccountDto eurAccount = registerAccount(Currency.EUR);

		// USD <-> KRW 간 거래 요청 생성
		TransactionRequestDto USDKRWRequest = TransactionRequestDto.builder()
			.senderId(krwAccount.id())
			.receiverId(usdAccount.id())
			.amount(new BigDecimal(2))
			.build();

		// EUR <-> KRW 간 거래 요청 생성
		TransactionRequestDto EURKRWRequest = TransactionRequestDto.builder()
			.senderId(krwAccount.id())
			.receiverId(eurAccount.id())
			.amount(new BigDecimal(2))
			.build();

		// 멀티스레드 환경 설정
		int threadCount = 10;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch latch = new CountDownLatch(threadCount);

		List<Future<Void>> futures = new ArrayList<>();

		// USD <-> KRW 작업 생성
		for(int i = 0; i < threadCount; i++) {
			futures.add(executorService.submit(() -> {
				try {
					transactionService.transfer(USDKRWRequest);
				} finally {
					latch.countDown();
				}
				return null;
			}));
		}

		// EUR <-> KRW 작업 생성
		for(int i = 0; i < threadCount; i++) {
			futures.add(executorService.submit(() -> {
				try {
					transactionService.transfer(EURKRWRequest);
				} finally {
					latch.countDown();
				}
				return null;
			}));
		}

		// 모든 작업 실행
		for (Future<Void> future : futures) {
			try {
				future.get(); // 각 future 실행 및 예외 대기
			} catch (ExecutionException e) {
				fail("Task execution failed: " + e.getMessage());
			}
		}
		latch.await(5, TimeUnit.SECONDS);
		executorService.shutdown();

		// USD <-> KRW 환율 조회 1회 확인
		verify(exchangeRateNaverService, times(1))
			.getExchangeRate(Currency.USD, Currency.KRW);

		// EUR <-> KRW 환율 조회 1회 확인
		verify(exchangeRateNaverService, times(1))
			.getExchangeRate(Currency.EUR, Currency.KRW);
	}

	private AccountDto registerAccount(Currency currency) {
		AccountRegisterRequestDto request = AccountRegisterRequestDto.builder()
			.money(new BigDecimal(100000))
			.currency(currency)
			.build();

		return accountService.register(request);
	}
}