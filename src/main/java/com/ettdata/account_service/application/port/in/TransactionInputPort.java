package com.ettdata.account_service.application.port.in;

import com.ettdata.account_service.domain.model.TransactionListResponse;
import com.ettdata.account_service.domain.model.TransactionResponse;
import com.ettdata.account_service.infrastructure.model.DepositRequest;
import reactor.core.publisher.Mono;

public interface TransactionInputPort {

    Mono<TransactionListResponse> getAllTransactionsByAccountNumber(String accountNumber);
    Mono<TransactionResponse> deposit(DepositRequest transactionResponse);
}
