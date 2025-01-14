package com.nayoung.exchangerateopenapitest.api;

import com.nayoung.exchangerateopenapitest.api.dto.account.AccountDto;
import com.nayoung.exchangerateopenapitest.api.dto.account.AccountRegisterRequestDto;
import com.nayoung.exchangerateopenapitest.api.dto.transaction.TransactionLogDto;
import com.nayoung.exchangerateopenapitest.api.dto.transaction.TransactionRequestDto;
import com.nayoung.exchangerateopenapitest.domain.account.service.AccountService;
import com.nayoung.exchangerateopenapitest.domain.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
public class BankController {

	private final AccountService accountService;
	private final TransactionService transactionService;

	@PostMapping("/accounts")
	public ResponseEntity<?> registerAccount(@RequestBody @Valid AccountRegisterRequestDto accountRegisterRequestDto) {
		AccountDto response = accountService.register(accountRegisterRequestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/accounts/{id}")
	public ResponseEntity<?> findAccountById(@PathVariable Long id) {
		AccountDto response = accountService.findAccountById(id);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/transfer")
	public ResponseEntity<?> transfer(@RequestBody @Valid TransactionRequestDto transactionRequestDto) {
		TransactionLogDto response = transactionService.transfer(transactionRequestDto);
		return ResponseEntity.ok(response);
	}
}
