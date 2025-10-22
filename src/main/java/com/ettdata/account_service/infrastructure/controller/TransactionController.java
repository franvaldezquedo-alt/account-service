package com.ettdata.account_service.infrastructure.controller;

import com.ettdata.account_service.application.port.in.TransactionInputService;
import com.ettdata.account_service.domain.model.TransactionResponse;
import com.ettdata.account_service.infrastructure.model.DepositRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionInputService transactionInputService;

    public TransactionController(TransactionInputService transactionInputService) {
        this.transactionInputService = transactionInputService;
    }

    @PostMapping("/save")
    Mono<TransactionResponse> deposit(@Valid  @RequestBody  DepositRequest depositRequest) {
        return transactionInputService.deposit(depositRequest);
    }
}
