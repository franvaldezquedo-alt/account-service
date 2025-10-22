package com.ettdata.account_service.infrastructure.entity;

import com.ettdata.account_service.domain.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Document(collection = "transactions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {
    @Id
    private String transactionId;
    private String accountNumber;
    private LocalDateTime transactionDate;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;
}
