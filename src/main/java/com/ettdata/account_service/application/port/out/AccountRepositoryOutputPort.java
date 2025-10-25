package com.ettdata.account_service.application.port.out;

import com.ettdata.account_service.domain.model.Account;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AccountRepositoryOutputPort {
    Flux<Account> findAllBankAccount();
    Mono<Account> saveAccount(Account account);
    Mono<Account> findByIdBankAccount(String id);
    Flux<Account> findByCustomerId(String customerId);
    Mono<Void> deleteByIdBankAccount(String id);
}
