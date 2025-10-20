package com.ettdata.account_service.infrastructure.repository;

import com.ettdata.account_service.domain.model.BankAccount;
import com.ettdata.account_service.infrastructure.entity.BankAccountEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface BankAccountRepository extends ReactiveMongoRepository<BankAccountEntity,String> {

    Flux<BankAccountEntity> findByCustomerId(String customerId);
    Flux<BankAccountEntity> findByCustomerIdAndAccountType(String customerId,
                                                           com.ettdata.account_service.domain.model.AccountType accountType);
}
