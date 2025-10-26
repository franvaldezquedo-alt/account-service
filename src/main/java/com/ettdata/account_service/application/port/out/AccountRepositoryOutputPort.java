package com.ettdata.account_service.application.port.out;

import com.ettdata.account_service.domain.model.Account;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountRepositoryOutputPort {
    Flux<Account> findAllAccount();

    Mono<Account> saveOrUpdateAccount(Account account);

    Mono<Account> findByIdAccount(String id);

    Flux<Account> findByCustomerId(String customerId);

    Mono<Void> deleteByIdAccount(String id);

    Mono<Account> findByNumberAccount(String numberAccount);
}