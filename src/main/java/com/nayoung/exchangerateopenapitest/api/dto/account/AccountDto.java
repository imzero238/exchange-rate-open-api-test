package com.nayoung.exchangerateopenapitest.api.dto.account;

import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record AccountDto (
	Long id,
	BigDecimal money,
	Currency currency
) {
}
