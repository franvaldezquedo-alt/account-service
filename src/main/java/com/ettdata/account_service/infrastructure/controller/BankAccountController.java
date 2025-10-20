package com.ettdata.account_service.infrastructure.controller;

import com.ettdata.account_service.application.port.in.BankAccountInputPort;
import com.ettdata.account_service.domain.model.BankAccountListResponse;
import com.ettdata.account_service.domain.model.BankAccountResponse;
import com.ettdata.account_service.infrastructure.model.BankAccountRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/bank-accounts")
@Slf4j
public class BankAccountController {

    private final BankAccountInputPort bankAccountInputPort;

    public BankAccountController(BankAccountInputPort bankAccountInputPort) {
        this.bankAccountInputPort = bankAccountInputPort;
    }

    @GetMapping("/all")
    Mono<BankAccountListResponse> getAllBankAccounts() {
        return bankAccountInputPort.findAllBankAccount()
                .doOnSuccess(res -> log.info("Respuesta lista de cuentas bancarias: {}", res))
                .doOnError(err -> log.error("Error al obtener lista de cuentas bancarias: {}", err.getMessage()));
    }

    @PostMapping("/save")
    Mono<BankAccountResponse> saveBankAccount(@RequestBody BankAccountRequest request) {
        return bankAccountInputPort.createBankAccount(request)
                .doOnSuccess(res -> log.info("Respuesta creación de cuenta: {}", res))
                .doOnError(err -> log.error("Error al crear cuenta bancaria: {}", err.getMessage()));
    }


}
