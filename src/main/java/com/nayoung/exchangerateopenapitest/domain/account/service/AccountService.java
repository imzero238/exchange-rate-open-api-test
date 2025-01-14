package com.nayoung.exchangerateopenapitest.domain.account.service;

import com.nayoung.exchangerateopenapitest.api.dto.account.AccountDto;
import com.nayoung.exchangerateopenapitest.api.dto.account.AccountRegisterRequestDto;
import com.nayoung.exchangerateopenapitest.domain.account.Account;
import com.nayoung.exchangerateopenapitest.domain.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

	private final AccountRepository accountRepository;

	public AccountDto register(AccountRegisterRequestDto accountRegisterRequestDto) {
		Account account = Account.builder()
			.money(accountRegisterRequestDto.money())
			.currency(accountRegisterRequestDto.currency())
			.build();

		accountRepository.save(account);

		return AccountDto.builder()
			.id(account.getId())
			.money(account.getMoney())
			.currency(account.getCurrency())
			.build();
	}

	public AccountDto findAccountById(Long id) {
		Account account = accountRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Account not found"));

		return AccountDto.builder()
			.id(account.getId())
			.money(account.getMoney())
			.currency(account.getCurrency())
			.build();
	}
}
