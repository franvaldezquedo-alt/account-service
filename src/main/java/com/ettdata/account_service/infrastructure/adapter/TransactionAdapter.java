package com.ettdata.account_service.infrastructure.adapter;

import com.ettdata.account_service.application.port.out.TransactionRepositoryOutputPort;
import com.ettdata.account_service.domain.model.Transaction;
import com.ettdata.account_service.infrastructure.entity.TransactionEntity;
import com.ettdata.account_service.infrastructure.repository.TransactionRepository;
import com.ettdata.account_service.infrastructure.utils.TransactionMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class TransactionAdapter implements TransactionRepositoryOutputPort {

  private final TransactionRepository transactionRepository;
  private final TransactionMapper transactionMapper;

  public TransactionAdapter(TransactionRepository transactionRepository, TransactionMapper transactionMapper) {
    this.transactionRepository = transactionRepository;
    this.transactionMapper = transactionMapper;
  }

  @Override
  public Flux<Transaction> findAllTransactionByAccountNumber(String accountNumber) {
    return transactionRepository.findAllTransactionByAccountNumber(accountNumber)
          .map(transactionMapper::toDomain);
  }

  @Override
  public Mono<Transaction> saveTransaction(Transaction transaction) {
    TransactionEntity entity = transactionMapper.toEntity(transaction);
    return transactionRepository.save(entity)
          .map(transactionMapper::toDomain);
  }
}
