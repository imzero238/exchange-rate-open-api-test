package com.nayoung.exchangerateopenapitest.api.dto.account;

import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AccountRegisterRequestDto (
	@NotNull(message = "Money must not be null.")
	@Min(value = 0, message = "Money must be at least 0.")
	BigDecimal money,

	@NotNull(message = "Currency must not be null.")
	Currency currency
){
}
