package com.ettdata.account_service.infrastructure.controller;

import com.ettdata.account_service.application.port.in.TransactionInputPort;
import com.ettdata.account_service.domain.model.TransactionListResponse;
import com.ettdata.account_service.domain.model.TransactionResponse;
import com.ettdata.account_service.infrastructure.model.DepositRequest;
import com.ettdata.account_service.infrastructure.model.TransferRequest;
import com.ettdata.account_service.infrastructure.model.WithdrawalRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionInputPort transactionInputService;

    public TransactionController(TransactionInputPort transactionInputService) {
        this.transactionInputService = transactionInputService;
    }

    @PostMapping("/save")
    Mono<TransactionResponse> deposit(@Valid  @RequestBody  DepositRequest depositRequest) {
        return transactionInputService.deposit(depositRequest);
    }

    @GetMapping("/{accountNumber}")
    Mono<TransactionListResponse> getAllTransactionsByAccountNumber(@PathVariable String accountNumber) {
        return transactionInputService.getAllTransactionsByAccountNumber(accountNumber);
    }

  @PostMapping("/transfer")
  Mono<TransactionResponse> transfer(@Valid  @RequestBody  TransferRequest transferRequest) {
    return transactionInputService.transfer(transferRequest);
  }

  @PostMapping("/withdraw")
  Mono<TransactionResponse> withdraw(@Valid  @RequestBody WithdrawalRequest withdrawalRequest) {
    return transactionInputService.withdraw(withdrawalRequest);
  }
}
