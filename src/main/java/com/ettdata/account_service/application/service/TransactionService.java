package com.ettdata.account_service.application.service;

import com.ettdata.account_service.application.port.in.TransactionInputService;
import com.ettdata.account_service.application.port.out.AccountRepositoryOutputPort;
import com.ettdata.account_service.application.port.out.TransactionRepositoryOutputPort;
import com.ettdata.account_service.domain.model.Transaction;
import com.ettdata.account_service.domain.model.TransactionListResponse;
import com.ettdata.account_service.domain.model.TransactionResponse;
import com.ettdata.account_service.infrastructure.model.DepositRequest;
import com.ettdata.account_service.infrastructure.utils.TransactionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@Slf4j
public class TransactionService implements TransactionInputService {

    private final AccountRepositoryOutputPort accountRepositoryOutputPort;
    private final TransactionRepositoryOutputPort transactionRepositoryOutputPort;

    public TransactionService(AccountRepositoryOutputPort accountRepositoryOutputPort,
                              TransactionRepositoryOutputPort transactionRepositoryOutputPort) {
        this.accountRepositoryOutputPort = accountRepositoryOutputPort;
        this.transactionRepositoryOutputPort = transactionRepositoryOutputPort;
    }

    @Override
    public Mono<TransactionListResponse> getTransactionsByAccountId(String accountId) {
        return null;
    }

    @Override
    public Mono<TransactionResponse> deposit(DepositRequest depositRequest) {
        log.info("Iniciando depósito en cuenta: {}", depositRequest.getNumberAccount());

        if (depositRequest.getAmount() == null || depositRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.just(TransactionUtils.createErrorResponse(400, "Deposit amount must be greater than zero"));
        }

        return accountRepositoryOutputPort.findByIdBankAccount(depositRequest.getNumberAccount())
                .flatMap(account -> {
                    if (!"ACTIVE".equalsIgnoreCase(account.getAccountStatus().name())) {
                        log.warn("Cuenta {} no está activa", account.getId());
                        return Mono.just(TransactionUtils.createErrorResponse(400, "Account is not active"));
                    }

                    BigDecimal newBalance = account.getBalance().add(depositRequest.getAmount());
                    account.setBalance(newBalance);

                    Transaction transaction = TransactionUtils.convertDepositRequestToDomain(depositRequest);
                    account.getTransactionList().add(transaction);

                    return accountRepositoryOutputPort.saveAccount(account)
                            .flatMap(savedAccount ->
                                    transactionRepositoryOutputPort.saveTransaction(transaction)
                                            .thenReturn(TransactionUtils.createSuccessResponse(
                                                    "Deposit successful. New balance: " + savedAccount.getBalance(),
                                                    transaction.getTransactionId()))
                            );
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Cuenta no encontrada con número: {}", depositRequest.getNumberAccount());
                    return Mono.just(TransactionUtils.createErrorResponse(404, "Account not found"));
                }))
                .onErrorResume(ex -> {
                    log.error("Error durante el depósito: {}", ex.getMessage());
                    return Mono.just(TransactionUtils.createErrorResponse(500, "Error processing deposit: " + ex.getMessage()));
                });
    }
}
