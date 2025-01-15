package com.nayoung.exchangerateopenapitest.domain.transaction.service;

import com.nayoung.exchangerateopenapitest.api.dto.transaction.TransactionLogDto;
import com.nayoung.exchangerateopenapitest.api.dto.transaction.TransactionRequestDto;
import com.nayoung.exchangerateopenapitest.domain.account.Account;
import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import com.nayoung.exchangerateopenapitest.domain.account.repository.AccountRepository;
import com.nayoung.exchangerateopenapitest.domain.exchangerate.naver.ExchangeRateService;
import com.nayoung.exchangerateopenapitest.domain.transaction.TransactionLog;
import com.nayoung.exchangerateopenapitest.domain.transaction.TransactionType;
import com.nayoung.exchangerateopenapitest.domain.transaction.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService {

	private final TransactionLogRepository transactionLogRepository;
	private final AccountRepository accountRepository;
	private final ExchangeRateService exchangeRateService;

	public TransactionLogDto transfer(TransactionRequestDto transactionRequestDto) {
		Account senderAccount = accountRepository.findById(transactionRequestDto.senderId())
			.orElseThrow(() -> new RuntimeException("Sender Account not found"));

		Account receiverAccount = accountRepository.findById(transactionRequestDto.receiverId())
			.orElseThrow(() -> new RuntimeException("Receiver Account not found"));

		BigDecimal exchangeRate = new BigDecimal(1);
		if(senderAccount.getCurrency() != receiverAccount.getCurrency()) {
			exchangeRate = exchangeRateService.getExchangeRate(receiverAccount.getCurrency(), senderAccount.getCurrency());
			if(exchangeRate.compareTo(BigDecimal.ZERO) <= 0) {
				throw new IllegalArgumentException("Invalid exchange rate");
			}
		}
		BigDecimal expectedWithdrawalAmount = exchangeRate.multiply(transactionRequestDto.amount());
		BigDecimal senderBalanceAfterTransaction = senderAccount.updateMoney(TransactionType.withdrawal, expectedWithdrawalAmount);
		BigDecimal receiverBalanceAfterTransaction = receiverAccount.updateMoney(TransactionType.deposit, transactionRequestDto.amount());

		saveTransactionLog(
			senderAccount.getId(), receiverAccount.getId(),
			senderAccount.getCurrency(), receiverAccount.getCurrency(),
			exchangeRate, expectedWithdrawalAmount, transactionRequestDto.amount(),
			senderBalanceAfterTransaction, receiverBalanceAfterTransaction);

		return TransactionLogDto.builder()
			.sendId(senderAccount.getId())
			.receiverId(receiverAccount.getId())
			.exchangeRate(exchangeRate)
			.amount(expectedWithdrawalAmount)
			.balanceAfterTransaction(senderBalanceAfterTransaction)
			.build();
	}

	private void saveTransactionLog(Long senderId, Long receiverId,
									Currency senderCurrency, Currency receiverCurrency,
									BigDecimal exchangeRate, BigDecimal expectedWithdrawalAmount, BigDecimal requestedTransferAmount,
									BigDecimal senderBalanceAfterTransaction, BigDecimal receiverBalanceAfterTransaction) {

		transactionLogRepository.save(TransactionLog.builder()
			.type(TransactionType.withdrawal)
			.senderId(senderId)
			.receiverId(receiverId)
			.currency(receiverCurrency)
			.exchangeRate(exchangeRate)
			.amount(expectedWithdrawalAmount)
			.balanceAfterTransaction(senderBalanceAfterTransaction)
			.build());

		transactionLogRepository.save(TransactionLog.builder()
			.type(TransactionType.deposit)
			.senderId(senderId)
			.receiverId(receiverId)
			.currency(senderCurrency)
			.exchangeRate(exchangeRate)
			.amount(requestedTransferAmount)
			.balanceAfterTransaction(receiverBalanceAfterTransaction)
			.build());
	}
}
