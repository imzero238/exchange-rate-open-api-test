package com.nayoung.exchangerateopenapitest.api.dto.transaction;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TransactionRequestDto (
	@NotNull(message = "SenderId must not be null.")
	@Min(value = 1, message = "SenderId must be at least 1.")
	Long senderId,

	@NotNull(message = "ReceiverId must not be null.")
	@Min(value = 1, message = "ReceiverId must be at least 1.")
	Long receiverId,

	@NotNull(message = "Amount must not be null.")
	@Min(value = 1, message = "Amount must be at least 1.")
	BigDecimal amount
) {
}
