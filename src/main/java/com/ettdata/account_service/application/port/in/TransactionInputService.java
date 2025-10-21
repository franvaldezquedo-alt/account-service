package com.ettdata.account_service.application.port.in;

import com.ettdata.account_service.domain.model.TransactionListResponse;
import com.ettdata.account_service.domain.model.TransactionResponse;
import reactor.core.publisher.Mono;

public interface TransactionInputService {

    Mono<TransactionListResponse> getTransactionsByAccountId(String accountId);
    Mono<TransactionResponse> createTransaction(TransactionResponse transactionResponse);
}
