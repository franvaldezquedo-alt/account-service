package com.ettdata.account_service.infrastructure.adapter;

import com.ettdata.account_service.application.port.out.TransactionRepositoryOutputPort;
import com.ettdata.account_service.domain.model.Transaction;
import com.ettdata.account_service.infrastructure.entity.TransactionEntity;
import com.ettdata.account_service.infrastructure.repository.TransactionRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class TransactionAdapter implements TransactionRepositoryOutputPort {

    private final TransactionRepository transactionRepository;

    public TransactionAdapter(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Flux<TransactionEntity> findAllTransactionByAccountNumber(String accountNumber) {
        return transactionRepository.findAllTransactionByAccountNumber(accountNumber);
    }

    @Override
    public Mono<Transaction> saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }
}
