package com.nayoung.exchangerateopenapitest.domain.transaction.service;

import com.nayoung.exchangerateopenapitest.api.dto.transaction.TransactionLogDto;
import com.nayoung.exchangerateopenapitest.api.dto.transaction.TransactionRequestDto;
import com.nayoung.exchangerateopenapitest.domain.account.Account;
import com.nayoung.exchangerateopenapitest.domain.account.repository.AccountRepository;
import com.nayoung.exchangerateopenapitest.domain.transaction.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionService {

	private final TransactionLogRepository transactionLogRepository;
	private final AccountRepository accountRepository;

	public TransactionLogDto transfer(TransactionRequestDto transactionRequestDto) {
		Account senderAccount = accountRepository.findById(transactionRequestDto.senderId())
			.orElseThrow(() -> new RuntimeException("Sender Account not found"));

		Account receiverAccount = accountRepository.findById(transactionRequestDto.receiverId())
			.orElseThrow(() -> new RuntimeException("Receiver Account not found"));

		// TODO: 환율 open api 구현

		return TransactionLogDto.builder()
			.sendId(senderAccount.getId())
			.receiverId(receiverAccount.getId())
			.exchangeRate(null)
			.amount(transactionRequestDto.amount())
			.balanceAfterTransaction(null)
			.build();
	}
}
