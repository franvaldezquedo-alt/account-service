package com.ettdata.account_service.application.port.in;

import com.ettdata.account_service.domain.model.AccountListResponse;
import com.ettdata.account_service.domain.model.AccountResponse;
import com.ettdata.account_service.infrastructure.model.AccountRequest;
import reactor.core.publisher.Mono;

public interface AccountInputPort {
    Mono<AccountListResponse> findAllBankAccount();
    Mono<AccountResponse> createBankAccount(AccountRequest bankAccountRequest);
    Mono<AccountListResponse> findByIdAccount(String id);
    Mono<AccountResponse> deleteByIdAccount(String id);
    Mono<AccountListResponse> findByNumberAccount(String numberAccount);
}
