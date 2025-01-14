package com.nayoung.exchangerateopenapitest.domain.transaction.repository;

import com.nayoung.exchangerateopenapitest.domain.transaction.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
}
