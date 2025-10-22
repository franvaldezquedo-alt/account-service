package com.ettdata.account_service.infrastructure.utils;

import com.ettdata.account_service.domain.model.Transaction;
import com.ettdata.account_service.domain.model.TransactionResponse;
import com.ettdata.account_service.domain.model.TransactionType;
import com.ettdata.account_service.infrastructure.entity.TransactionEntity;
import com.ettdata.account_service.infrastructure.model.DepositRequest;

import java.time.LocalDateTime;

public class TransactionUtils {
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

}
