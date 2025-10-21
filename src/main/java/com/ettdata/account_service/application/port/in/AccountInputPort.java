package com.ettdata.account_service.application.port.in;

import com.ettdata.account_service.domain.model.AccountListResponse;
import com.ettdata.account_service.domain.model.AccountResponse;
import com.ettdata.account_service.infrastructure.model.AccountRequest;
import reactor.core.publisher.Mono;

public interface AccountInputPort {
    Mono<AccountListResponse> findAllBankAccount();
    Mono<AccountResponse> createBankAccount(AccountRequest bankAccountRequest);
    Mono<AccountListResponse> findByIdBankAccount(String id);
    Mono<AccountResponse> deleteByIdBankAccount(String id);
}
