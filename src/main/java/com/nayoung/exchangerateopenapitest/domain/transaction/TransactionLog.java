package com.nayoung.exchangerateopenapitest.domain.transaction;

import com.nayoung.exchangerateopenapitest.domain.account.Currency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransactionType type;

	@Column(nullable = false)
	private Long senderId;

	@Column(nullable = false)
	private Long receiverId;

	@Column(nullable = false)
	private Currency currency;

	@Column(nullable = false)
	private BigDecimal exchangeRate;

	@Column(nullable = false)
	private Long amount;

	@Column(nullable = false)
	private Long balanceAfterTransaction;

	@CreatedDate
	private LocalDateTime createdAt;
}
