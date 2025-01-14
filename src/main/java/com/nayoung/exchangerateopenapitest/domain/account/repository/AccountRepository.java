package com.nayoung.exchangerateopenapitest.domain.account.repository;

import com.nayoung.exchangerateopenapitest.domain.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
