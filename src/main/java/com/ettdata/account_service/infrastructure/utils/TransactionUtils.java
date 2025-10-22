package com.ettdata.account_service.infrastructure.utils;

import com.ettdata.account_service.domain.model.Transaction;
import com.ettdata.account_service.domain.model.TransactionListResponse;
import com.ettdata.account_service.domain.model.TransactionResponse;
import com.ettdata.account_service.domain.model.TransactionType;
import com.ettdata.account_service.infrastructure.entity.TransactionEntity;
import com.ettdata.account_service.infrastructure.model.DepositRequest;
import com.ettdata.account_service.infrastructure.model.TransferRequest;
import com.ettdata.account_service.infrastructure.model.WithdrawalRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TransactionUtils {


  public static TransactionListResponse convertToTransactionListResponse(List<TransactionEntity> entities) {
    if (entities == null || entities.isEmpty()) {
      return TransactionListResponse.builder()
            .data(Collections.emptyList())
            .Error(null)
            .build();
    }

    return TransactionListResponse.builder()
          .data(entities.stream()
                .map(TransactionUtils::convertToCustomerResponse)
                .collect(Collectors.toList()))
          .Error(null)
          .build();
  }

  public static Transaction convertToCustomerResponse(TransactionEntity entity) {
    return Transaction.builder()
          .transactionId(entity.getTransactionId())
          .accountNumber(entity.getAccountNumber())
          .transactionType(entity.getTransactionType())
          .amount(entity.getAmount())
          .transactionDate(entity.getTransactionDate())
          .description(entity.getDescription())
          .build();
  }


    /** Convierte un DepositRequest en una entidad Transaction */
    public static Transaction convertDepositRequestToDomain(DepositRequest request) {
        return Transaction.builder()
                .transactionId("TX-" + System.currentTimeMillis())
                .accountNumber(request.getNumberAccount())
                .transactionType(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .transactionDate(LocalDateTime.now())
                .description(
                        request.getDescription() != null
                                ? request.getDescription()
                                : "Cash deposit")
                .build();
    }

    /** Convierte una entidad a modelo de dominio */
    public static Transaction convertEntityToDomain(TransactionEntity entity) {
        if (entity == null) return null;

        return Transaction.builder()
                .transactionId(entity.getTransactionId())
                .transactionType(entity.getTransactionType())
                .amount(entity.getAmount())
                .transactionDate(entity.getTransactionDate())
                .description(entity.getDescription())
                .build();
    }

    /** Convierte un dominio a entidad */
    public static TransactionEntity convertDomainToEntity(Transaction domain) {
        if (domain == null) return null;

        return TransactionEntity.builder()
                .transactionId(domain.getTransactionId())
                .transactionType(domain.getTransactionType())
                .amount(domain.getAmount())
                .transactionDate(domain.getTransactionDate())
                .description(domain.getDescription())
                .build();
    }

    /** Genera respuesta exitosa */
    public static TransactionResponse createSuccessResponse(String message, String transactionId) {
        return TransactionResponse.builder()
                .codResponse(200)
                .messageResponse(message)
                .codEntity(transactionId)
                .build();
    }

    /** Genera respuesta de error */
    public static TransactionResponse createErrorResponse(int code, String message) {
        return TransactionResponse.builder()
                .codResponse(code)
                .messageResponse(message)
                .codEntity(null)
                .build();
    }



  public static Transaction createTransferOutTransaction(TransferRequest request) {
    return Transaction.builder()
          .transactionId(UUID.randomUUID().toString())
          .accountNumber(request.getSourceNumberAccount())
          .amount(request.getAmount().negate()) // salida
          .transactionType(TransactionType.TRANSFER)
          .description(request.getDescription() != null ? request.getDescription() : "Transfer to " + request.getTargetNumberAccount())
          .transactionDate(LocalDateTime.now())
          .build();
  }

  public static Transaction createTransferInTransaction(TransferRequest request) {
    return Transaction.builder()
          .transactionId(UUID.randomUUID().toString())
          .accountNumber(request.getTargetNumberAccount())
          .amount(request.getAmount()) // entrada
          .transactionType(TransactionType.TRANSFER)
          .description(request.getDescription() != null ? request.getDescription() : "Transfer from " + request.getSourceNumberAccount())
          .transactionDate(LocalDateTime.now())
          .build();
  }

  public static Transaction createWithdrawalTransaction(WithdrawalRequest request) {
    return Transaction.builder()
          .transactionId(UUID.randomUUID().toString())
          .accountNumber(request.getNumberAccount())
          .amount(request.getAmount().negate()) // negativo porque es un retiro
          .transactionType(TransactionType.WITHDRAWAL)
          .description(request.getDescription() != null ? request.getDescription() : "Withdrawal")
          .transactionDate(LocalDateTime.now())
          .build();
  }



}
