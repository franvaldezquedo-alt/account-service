package com.ettdata.account_service.application.port.out;

import com.ettdata.account_service.domain.model.Transaction;
import com.ettdata.account_service.infrastructure.entity.TransactionEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionRepositoryOutputPort {
    Flux<TransactionEntity> findAllTransactionByAccountId(String accountId);
    Mono<Transaction> saveTransaction(Transaction transaction);
}
