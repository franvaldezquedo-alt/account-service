package com.ettdata.account_service.infrastructure.repository;

import com.ettdata.account_service.domain.model.Transaction;
import com.ettdata.account_service.infrastructure.entity.TransactionEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {
    Flux<TransactionEntity> findAllTransactionByAccountNumber(String accountNumber);
}
