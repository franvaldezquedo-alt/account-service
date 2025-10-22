package com.ettdata.account_service.infrastructure.repository;

import com.ettdata.account_service.infrastructure.entity.AccountEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AccountRepository extends ReactiveMongoRepository<AccountEntity,String> {

    Flux<AccountEntity> findByCustomerId(String customerId);
    Flux<AccountEntity> findByCustomerIdAndAccountType(String customerId,
                                                       com.ettdata.account_service.domain.model.AccountType accountType);
    Mono<AccountEntity> findByAccountNumber(String accountNumber);
}
