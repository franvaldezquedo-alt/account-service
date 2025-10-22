package com.ettdata.account_service.infrastructure.utils;

import com.ettdata.account_service.domain.model.Transaction;
import com.ettdata.account_service.domain.model.TransactionListResponse;
import com.ettdata.account_service.domain.model.TransactionResponse;
import com.ettdata.account_service.domain.model.TransactionType;
import com.ettdata.account_service.infrastructure.entity.TransactionEntity;
import com.ettdata.account_service.infrastructure.model.DepositRequest;
import com.ettdata.account_service.infrastructure.model.TransferRequest;
import com.ettdata.account_service.infrastructure.model.WithdrawalRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper component for Transaction conversions between layers
 */
@Component
public class TransactionMapper {

  private static final String DEFAULT_DEPOSIT_DESC = "Cash deposit";
  private static final String DEFAULT_WITHDRAWAL_DESC = "Withdrawal";
  private static final String TRANSFER_TO_PREFIX = "Transfer to ";
  private static final String TRANSFER_FROM_PREFIX = "Transfer from ";

  // ===== Entity to Domain =====

  public Transaction toDomain(TransactionEntity entity) {
    if (entity == null) {
      return null;
    }

    return Transaction.builder()
          .transactionId(entity.getTransactionId())
          .accountNumber(entity.getAccountNumber())
          .transactionType(entity.getTransactionType())
          .amount(entity.getAmount())
          .transactionDate(entity.getTransactionDate())
          .description(entity.getDescription())
          .build();
  }

  public Transaction toDomain(Transaction transaction) {
    // Identity mapping for Transaction domain objects
    return transaction;
  }

  public List<Transaction> toDomainList(List<?> items) {
    if (items == null || items.isEmpty()) {
      return Collections.emptyList();
    }

    return items.stream()
          .map(item -> {
            if (item instanceof TransactionEntity) {
              return toDomain((TransactionEntity) item);
            } else if (item instanceof Transaction) {
              return (Transaction) item;
            }
            return null;
          })
          .collect(Collectors.toList());
  }

  // ===== Domain to Entity =====

  public TransactionEntity toEntity(Transaction domain) {
    if (domain == null) {
      return null;
    }

    return TransactionEntity.builder()
          .transactionId(domain.getTransactionId())
          .accountNumber(domain.getAccountNumber())
          .transactionType(domain.getTransactionType())
          .amount(domain.getAmount())
          .transactionDate(domain.getTransactionDate())
          .description(domain.getDescription())
          .build();
  }

  public TransactionEntity toEntity(com.ettdata.account_service.infrastructure.entity.TransactionEntity entity) {
    // Identity mapping for TransactionEntity objects
    return entity;
  }

  public List<TransactionEntity> toEntityList(List<?> items) {
    if (items == null || items.isEmpty()) {
      return Collections.emptyList();
    }

    return items.stream()
          .map(item -> {
            if (item instanceof Transaction) {
              return toEntity((Transaction) item);
            } else if (item instanceof TransactionEntity) {
              return (TransactionEntity) item;
            }
            return null;
          })
          .collect(Collectors.toList());
  }

  // ===== Request to Domain =====

  public Transaction toDepositTransaction(DepositRequest request) {
    return Transaction.builder()
          .transactionId(generateTransactionId())
          .accountNumber(request.getNumberAccount())
          .transactionType(TransactionType.DEPOSIT)
          .amount(request.getAmount())
          .transactionDate(LocalDateTime.now())
          .description(request.getDescription())
          .build();
  }

  public Transaction toWithdrawalTransaction(WithdrawalRequest request) {
    return Transaction.builder()
          .transactionId(generateTransactionId())
          .accountNumber(request.getNumberAccount())
          .transactionType(TransactionType.WITHDRAWAL)
          .amount(request.getAmount().negate())
          .transactionDate(LocalDateTime.now())
          .description(getOrDefault(request.getDescription(), DEFAULT_WITHDRAWAL_DESC))
          .build();
  }

  public Transaction toTransferOutTransaction(TransferRequest request) {
    return buildTransferTransaction(
          request.getSourceNumberAccount(),
          request.getAmount().negate(),
          getOrDefault(request.getDescription(),
                TRANSFER_TO_PREFIX + request.getTargetNumberAccount())
    );
  }

  public Transaction toTransferInTransaction(TransferRequest request) {
    return buildTransferTransaction(
          request.getTargetNumberAccount(),
          request.getAmount(),
          getOrDefault(request.getDescription(),
                TRANSFER_FROM_PREFIX + request.getSourceNumberAccount())
    );
  }

  // ===== Response Builders =====

  public TransactionResponse toSuccessResponse(String message, String transactionId) {
    return TransactionResponse.builder()
          .codResponse(200)
          .messageResponse(message)
          .codEntity(transactionId)
          .build();
  }

  public TransactionResponse toErrorResponse(int code, String message) {
    return TransactionResponse.builder()
          .codResponse(code)
          .messageResponse(message)
          .codEntity(null)
          .build();
  }

  public TransactionListResponse toTransactionListResponse(List<Transaction> entities) {
    if (entities == null || entities.isEmpty()) {
      return TransactionListResponse.builder()
            .data(Collections.emptyList())
            .Error(null)
            .build();
    }

    List<Transaction> transactions = entities.stream()
          .map(this::toDomain)
          .collect(Collectors.toList());

    return TransactionListResponse.builder()
          .data(transactions)
          .Error(null)
          .build();
  }

  // ===== Private Helpers =====

  private Transaction buildTransferTransaction(String accountNumber,
                                               BigDecimal amount,
                                               String description) {
    return Transaction.builder()
          .transactionId(generateTransactionId())
          .accountNumber(accountNumber)
          .transactionType(TransactionType.TRANSFER)
          .amount(amount)
          .transactionDate(LocalDateTime.now())
          .description(description)
          .build();
  }

  private String generateTransactionId() {
    return UUID.randomUUID().toString();
  }

  private String getOrDefault(String value, String defaultValue) {
    return value != null ? value : defaultValue;
  }
}