package com.ettdata.account_service.infrastructure.model;

import com.ettdata.account_service.domain.model.TransactionType;

public class TransactionRequest {
    private TransactionType transactionType;
    private Double amount;
    private String description;

}
