package com.ettdata.account_service.application.port.out;

import com.ettdata.account_service.domain.model.BankAccount;
import com.ettdata.account_service.infrastructure.entity.BankAccountEntity;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BankAccountRepositoryOutputPort {
    Flux<BankAccountEntity> findAllBankAccount();
    Mono<BankAccount> saveBankAccount(BankAccount bankAccount);
    Mono<BankAccountEntity> findByIdBankAccount(String id);
    Flux<BankAccountEntity> findByCustomerId(String customerId);
}
