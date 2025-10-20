package com.ettdata.account_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    private String transactionId;
    private LocalDateTime transactionDate;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;
    private String targetAccountNumber;
}
