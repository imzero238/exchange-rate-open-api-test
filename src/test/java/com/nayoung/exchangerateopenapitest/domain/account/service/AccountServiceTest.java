package com.nayoung.exchangerateopenapitest.domain.account.service;

import com.nayoung.exchangerateopenapitest.api.dto.account.AccountRegisterRequestDto;
import com.nayoung.exchangerateopenapitest.domain.account.Account;
import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import com.nayoung.exchangerateopenapitest.domain.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	@InjectMocks
	private AccountService accountService;

	@Mock
	private AccountRepository accountRepository;

	@Test
	void 계좌_생성_성공_케이스() {
		Mockito.when(accountRepository.save(any()))
			.thenReturn(Mockito.mock(Account.class));

		AccountRegisterRequestDto request = AccountRegisterRequestDto.builder()
			.money(2000L)
			.currency(Currency.KRW)
			.build();

		accountService.register(request);

		verify(accountRepository, times(1))
			.save(any(Account.class));
	}
}