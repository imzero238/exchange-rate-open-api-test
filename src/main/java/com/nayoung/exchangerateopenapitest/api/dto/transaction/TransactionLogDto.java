package com.nayoung.exchangerateopenapitest.api.dto.transaction;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TransactionLogDto (
	Long sendId,
	Long receiverId,
	BigDecimal exchangeRate,
	BigDecimal amount,
	BigDecimal balanceAfterTransaction
) {
}
