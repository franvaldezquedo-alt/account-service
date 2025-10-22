package com.ettdata.account_service.application.port.out;

import com.ettdata.account_service.domain.model.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionRepositoryOutputPort {
    Flux<Transaction> findAllTransactionByAccountNumber(String accountNumber);
    Mono<Transaction> saveTransaction(Transaction transaction);
}
