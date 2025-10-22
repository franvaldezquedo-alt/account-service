package com.ettdata.account_service.application.service;

import com.ettdata.account_service.application.port.in.TransactionInputPort;
import com.ettdata.account_service.application.port.out.AccountRepositoryOutputPort;
import com.ettdata.account_service.application.port.out.TransactionRepositoryOutputPort;
import com.ettdata.account_service.domain.model.Transaction;
import com.ettdata.account_service.domain.model.TransactionListResponse;
import com.ettdata.account_service.domain.model.TransactionResponse;
import com.ettdata.account_service.infrastructure.model.DepositRequest;
import com.ettdata.account_service.infrastructure.model.TransferRequest;
import com.ettdata.account_service.infrastructure.model.WithdrawalRequest;
import com.ettdata.account_service.infrastructure.utils.TransactionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@Slf4j
public class TransactionService implements TransactionInputPort {

    private final AccountRepositoryOutputPort accountRepositoryOutputPort;
    private final TransactionRepositoryOutputPort transactionRepositoryOutputPort;

    public TransactionService(AccountRepositoryOutputPort accountRepositoryOutputPort,
                              TransactionRepositoryOutputPort transactionRepositoryOutputPort) {
        this.accountRepositoryOutputPort = accountRepositoryOutputPort;
        this.transactionRepositoryOutputPort = transactionRepositoryOutputPort;
    }

    @Override
    public Mono<TransactionListResponse> getAllTransactionsByAccountNumber(String accountNumber) {
        return transactionRepositoryOutputPort.findAllTransactionByAccountNumber(accountNumber)
              .collectList()
              .map(TransactionUtils::convertToTransactionListResponse)
              .doOnSuccess(res -> log.debug("Se encontraron {} transacciones para la cuenta {}",
                      res.getData() != null ? res.getData().size() : 0, accountNumber))
              .doOnError(error -> log.error("Error al consultar transacciones para la cuenta {}: {}", accountNumber, error.getMessage()));
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

  @Override
  public Mono<TransactionResponse> transfer(TransferRequest transferRequest) {
    log.info("Iniciando transferencia de {} desde cuenta {} hacia cuenta {}",
          transferRequest.getAmount(), transferRequest.getSourceNumberAccount(), transferRequest.getTargetNumberAccount());

    if (transferRequest.getAmount() == null || transferRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      return Mono.just(TransactionUtils.createErrorResponse(400, "Transfer amount must be greater than zero"));
    }

    if (transferRequest.getSourceNumberAccount().equals(transferRequest.getTargetNumberAccount())) {
      return Mono.just(TransactionUtils.createErrorResponse(400, "Source and target accounts must be different"));
    }

    return accountRepositoryOutputPort.findByIdBankAccount(transferRequest.getSourceNumberAccount())
          .flatMap(sourceAccount -> {
            if (!"ACTIVE".equalsIgnoreCase(sourceAccount.getAccountStatus().name())) {
              log.warn("Cuenta origen {} no está activa", sourceAccount.getAccountNumber());
              return Mono.just(TransactionUtils.createErrorResponse(400, "Source account is not active"));
            }

            if (sourceAccount.getBalance().compareTo(transferRequest.getAmount()) < 0) {
              log.warn("Saldo insuficiente en cuenta {}", sourceAccount.getAccountNumber());
              return Mono.just(TransactionUtils.createErrorResponse(400, "Insufficient funds"));
            }

            return accountRepositoryOutputPort.findByIdBankAccount(transferRequest.getTargetNumberAccount())
                  .flatMap(targetAccount -> {
                    if (!"ACTIVE".equalsIgnoreCase(targetAccount.getAccountStatus().name())) {
                      log.warn("Cuenta destino {} no está activa", targetAccount.getAccountNumber());
                      return Mono.just(TransactionUtils.createErrorResponse(400, "Target account is not active"));
                    }

                    // Actualizar saldos
                    BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(transferRequest.getAmount());
                    BigDecimal newTargetBalance = targetAccount.getBalance().add(transferRequest.getAmount());

                    sourceAccount.setBalance(newSourceBalance);
                    targetAccount.setBalance(newTargetBalance);

                    // Crear transacciones de salida y entrada
                    Transaction outTransaction = TransactionUtils.createTransferOutTransaction(transferRequest);
                    Transaction inTransaction = TransactionUtils.createTransferInTransaction(transferRequest);

                    sourceAccount.getTransactionList().add(outTransaction);
                    targetAccount.getTransactionList().add(inTransaction);

                    // Guardar cambios
                    return accountRepositoryOutputPort.saveAccount(sourceAccount)
                          .then(accountRepositoryOutputPort.saveAccount(targetAccount))
                          .then(transactionRepositoryOutputPort.saveTransaction(outTransaction))
                          .then(transactionRepositoryOutputPort.saveTransaction(inTransaction))
                          .thenReturn(TransactionUtils.createSuccessResponse(
                                "Transfer successful. New source balance: " + newSourceBalance,
                                outTransaction.getTransactionId()));
                  })
                  .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Cuenta destino no encontrada: {}", transferRequest.getTargetNumberAccount());
                    return Mono.just(TransactionUtils.createErrorResponse(404, "Target account not found"));
                  }));
          })
          .switchIfEmpty(Mono.defer(() -> {
            log.warn("Cuenta origen no encontrada: {}", transferRequest.getSourceNumberAccount());
            return Mono.just(TransactionUtils.createErrorResponse(404, "Source account not found"));
          }))
          .onErrorResume(ex -> {
            log.error("Error durante la transferencia: {}", ex.getMessage());
            return Mono.just(TransactionUtils.createErrorResponse(500, "Error processing transfer: " + ex.getMessage()));
          });
  }

  @Override
  public Mono<TransactionResponse> withdraw(WithdrawalRequest withdrawalRequest) {
    log.info("Iniciando retiro en cuenta: {}", withdrawalRequest.getNumberAccount());

    // 1. Validar monto
    if (withdrawalRequest.getAmount() == null || withdrawalRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
      return Mono.just(TransactionUtils.createErrorResponse(400, "Withdrawal amount must be greater than zero"));
    }

    // 2. Buscar cuenta
    return accountRepositoryOutputPort.findByIdBankAccount(withdrawalRequest.getNumberAccount())
          .flatMap(account -> {
            // 3. Verificar estado
            if (!"ACTIVE".equalsIgnoreCase(account.getAccountStatus().name())) {
              log.warn("Cuenta {} no está activa", account.getAccountNumber());
              return Mono.just(TransactionUtils.createErrorResponse(400, "Account is not active"));
            }

            // 4. Verificar saldo suficiente
            if (account.getBalance().compareTo(withdrawalRequest.getAmount()) < 0) {
              log.warn("Saldo insuficiente en cuenta {}", account.getAccountNumber());
              return Mono.just(TransactionUtils.createErrorResponse(400, "Insufficient funds"));
            }

            // 5. Actualizar saldo
            BigDecimal newBalance = account.getBalance().subtract(withdrawalRequest.getAmount());
            account.setBalance(newBalance);

            // 6. Crear transacción
            Transaction transaction = TransactionUtils.createWithdrawalTransaction(withdrawalRequest);
            account.getTransactionList().add(transaction);

            // 7. Guardar cuenta y transacción
            return accountRepositoryOutputPort.saveAccount(account)
                  .flatMap(savedAccount ->
                        transactionRepositoryOutputPort.saveTransaction(transaction)
                              .thenReturn(TransactionUtils.createSuccessResponse(
                                    "Withdrawal successful. New balance: " + savedAccount.getBalance(),
                                    transaction.getTransactionId()))
                  );
          })
          .switchIfEmpty(Mono.defer(() -> {
            log.warn("Cuenta no encontrada: {}", withdrawalRequest.getNumberAccount());
            return Mono.just(TransactionUtils.createErrorResponse(404, "Account not found"));
          }))
          .onErrorResume(ex -> {
            log.error("Error durante el retiro: {}", ex.getMessage());
            return Mono.just(TransactionUtils.createErrorResponse(500, "Error processing withdrawal: " + ex.getMessage()));
          });
  }

}
