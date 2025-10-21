package com.ettdata.account_service.application.port.out;

import com.ettdata.account_service.domain.model.Account;
import com.ettdata.account_service.infrastructure.entity.AccountEntity;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AccountRepositoryOutputPort {
    Flux<AccountEntity> findAllBankAccount();
    Mono<Account> saveBankAccount(Account account);
    Mono<AccountEntity> findByIdBankAccount(String id);
    Flux<AccountEntity> findByCustomerId(String customerId);
    Mono<Void> deleteByIdBankAccount(String id);
}
