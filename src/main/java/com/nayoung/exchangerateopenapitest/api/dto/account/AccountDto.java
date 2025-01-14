package com.nayoung.exchangerateopenapitest.api.dto.account;

import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import lombok.Builder;

@Builder
public record AccountDto (
	Long id,
	Long money,
	Currency currency
) {
}
