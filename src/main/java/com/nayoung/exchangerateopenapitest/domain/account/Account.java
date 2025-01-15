package com.nayoung.exchangerateopenapitest.domain.account;

import com.nayoung.exchangerateopenapitest.domain.transaction.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private BigDecimal money;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Currency currency;

	public BigDecimal updateMoney(TransactionType type, BigDecimal amount) {
		if(this.currency == Currency.KRW) {
			amount = amount.setScale(0, RoundingMode.CEILING);
		}
		if(type == TransactionType.deposit) {
			money = money.add(amount);
		} else if(type == TransactionType.withdrawal) {
			if(amount.compareTo(this.money) > 0) {
				throw new RuntimeException("Not enough money");
			}
			money = money.subtract(amount);
		}
		return this.money;
	}
}
