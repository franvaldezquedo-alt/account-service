package com.ettdata.account_service.infrastructure.controller;

import com.ettdata.account_service.application.port.in.AccountInputPort;
import com.ettdata.account_service.domain.model.AccountListResponse;
import com.ettdata.account_service.domain.model.AccountResponse;
import com.ettdata.account_service.infrastructure.model.AccountRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/bank-accounts")
@Slf4j
public class AccountController {

    private final AccountInputPort accountInputPort;

    public AccountController(AccountInputPort accountInputPort) {
        this.accountInputPort = accountInputPort;
    }

    @GetMapping("/all")
    Mono<AccountListResponse> getAllBankAccounts() {
        return accountInputPort.findAllBankAccount()
                .doOnSuccess(res -> log.info("Respuesta lista de cuentas bancarias: {}", res))
                .doOnError(err -> log.error("Error al obtener lista de cuentas bancarias: {}", err.getMessage()));
    }

    @GetMapping("/{id}")
    Mono<AccountListResponse> getBankAccountById(@PathVariable String id) {
        return accountInputPort.findByIdBankAccount(id)
                .doOnSuccess(res -> log.info("Respuesta cuenta bancaria por ID: {}", res));
    }

    @PostMapping("/save")
    Mono<AccountResponse> saveBankAccount(@RequestBody AccountRequest request) {
        return accountInputPort.createBankAccount(request)
                .doOnSuccess(res -> log.info("Respuesta creación de cuenta: {}", res))
                .doOnError(err -> log.error("Error al crear cuenta bancaria: {}", err.getMessage()));
    }

    @DeleteMapping("/delete/{id}")
    Mono<AccountResponse> deleteBankAccount(@PathVariable String id) {
        return accountInputPort.deleteByIdBankAccount(id);
    }

}
