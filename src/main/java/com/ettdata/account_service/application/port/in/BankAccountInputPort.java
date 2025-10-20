package com.ettdata.account_service.application.port.in;

import com.ettdata.account_service.domain.model.BankAccountListResponse;
import com.ettdata.account_service.domain.model.BankAccountResponse;
import com.ettdata.account_service.infrastructure.model.BankAccountRequest;
import reactor.core.publisher.Mono;

public interface BankAccountInputPort {
    Mono<BankAccountListResponse> findAllBankAccount();
    Mono<BankAccountResponse> createBankAccount(BankAccountRequest bankAccountRequest);
    Mono<BankAccountListResponse> findByIdBankAccount(String id);
    Mono<BankAccountResponse> deleteByIdBankAccount(String id);
}
