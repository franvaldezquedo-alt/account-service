package com.ettdata.account_service.application.service;

import com.ettdata.account_service.application.port.in.TransactionInputPort;
import com.ettdata.account_service.application.port.out.AccountRepositoryOutputPort;
import com.ettdata.account_service.application.port.out.TransactionRepositoryOutputPort;
import com.ettdata.account_service.domain.model.Account;
import com.ettdata.account_service.domain.model.Transaction;
import com.ettdata.account_service.domain.model.TransactionListResponse;
import com.ettdata.account_service.domain.model.TransactionResponse;
import com.ettdata.account_service.infrastructure.model.DepositRequest;
import com.ettdata.account_service.infrastructure.model.TransferRequest;
import com.ettdata.account_service.infrastructure.model.WithdrawalRequest;
import com.ettdata.account_service.infrastructure.utils.AccountMapper;
import com.ettdata.account_service.infrastructure.utils.TransactionMapper;
import com.ettdata.account_service.infrastructure.utils.TransactionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService implements TransactionInputPort {

  private static final String ACCOUNT_NOT_FOUND = "Account not found";
  private static final String ACCOUNT_NOT_ACTIVE = "Account is not active";

  private final AccountRepositoryOutputPort accountRepository;
  private final TransactionRepositoryOutputPort transactionRepository;
  private final TransactionValidator validator;
  private final TransactionMapper transactionMapper;
  private final AccountMapper accountMapper;

  @Override
  public Mono<TransactionListResponse> getAllTransactionsByAccountNumber(String accountNumber) {
    log.info("Retrieving transactions for account: {}", accountNumber);

    return transactionRepository.findAllTransactionByAccountNumber(accountNumber)
          .collectList()
          .map(transactionMapper::toTransactionListResponse)
          .doOnSuccess(response ->
                log.debug("Found {} transactions for account {}",
                      response.getData().size(), accountNumber))
          .doOnError(error ->
                log.error("Error retrieving transactions for account {}: {}",
                      accountNumber, error.getMessage()));
  }

  @Override
  public Mono<TransactionResponse> deposit(DepositRequest request) {
    log.info("Processing deposit for account: {}", request.getNumberAccount());

    return validator.validateAmount(request.getAmount())
          .then(findActiveAccount(request.getNumberAccount()))
          .flatMap(account -> processDeposit(account, request))
          .onErrorResume(this::handleError);
  }

  @Override
  public Mono<TransactionResponse> withdraw(WithdrawalRequest request) {
    log.info("Processing withdrawal for account: {}", request.getNumberAccount());

    return validator.validateAmount(request.getAmount())
          .then(findActiveAccount(request.getNumberAccount()))
          .flatMap(account -> validator.validateSufficientFunds(account, request.getAmount())
                .then(processWithdrawal(account, request)))
          .onErrorResume(this::handleError);
  }

  @Override
  public Mono<TransactionResponse> transfer(TransferRequest request) {
    log.info("Processing transfer from {} to {} amount: {}",
          request.getSourceNumberAccount(),
          request.getTargetNumberAccount(),
          request.getAmount());

    return validator.validateAmount(request.getAmount())
          .then(validator.validateDifferentAccounts(
                request.getSourceNumberAccount(),
                request.getTargetNumberAccount()))
          .then(findActiveAccount(request.getSourceNumberAccount()))
          .flatMap(sourceAccount ->
                validator.validateSufficientFunds(sourceAccount, request.getAmount())
                      .then(findActiveAccount(request.getTargetNumberAccount()))
                      .flatMap(targetAccount ->
                            processTransfer(sourceAccount, targetAccount, request)))
          .onErrorResume(this::handleError);
  }

  // ===== Private Helper Methods =====

  private Mono<Account> findActiveAccount(String accountNumber) {
    return accountRepository.findByIdBankAccount(accountNumber)
          .switchIfEmpty(Mono.error(new IllegalArgumentException(ACCOUNT_NOT_FOUND)))
          .flatMap(account -> {
            if (!"ACTIVE".equalsIgnoreCase(account.getAccountStatus().name())) {
              log.warn("Account {} is not active", accountNumber);
              return Mono.error(new IllegalStateException(ACCOUNT_NOT_ACTIVE));
            }
            return Mono.just(account);
          });
  }

  private Mono<TransactionResponse> processDeposit(Account account, DepositRequest request) {
    BigDecimal newBalance = account.getBalance().add(request.getAmount());
    account.setBalance(newBalance);

    Transaction transaction = transactionMapper.toDepositTransaction(request);

    log.info("Transaction created successfully: {}", transaction);


    return saveAccountAndTransaction(account, transaction)
          .map(savedAccount -> transactionMapper.toSuccessResponse(
                "Deposit successful. New balance: " + savedAccount.getBalance(),
                transaction.getTransactionId()));
  }

  private Mono<TransactionResponse> processWithdrawal(Account account, WithdrawalRequest request) {
    BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
    account.setBalance(newBalance);

    Transaction transaction = transactionMapper.toWithdrawalTransaction(request);

    return saveAccountAndTransaction(account, transaction)
          .map(savedAccount -> transactionMapper.toSuccessResponse(
                "Withdrawal successful. New balance: " + savedAccount.getBalance(),
                transaction.getTransactionId()));
  }

  private Mono<TransactionResponse> processTransfer(Account sourceAccount,
                                                    Account targetAccount,
                                                    TransferRequest request) {
    // Update balances
    sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
    targetAccount.setBalance(targetAccount.getBalance().add(request.getAmount()));

    // Create transactions
    Transaction outTransaction = transactionMapper.toTransferOutTransaction(request);
    Transaction inTransaction = transactionMapper.toTransferInTransaction(request);

    // Save everything
    return accountRepository.saveAccount(sourceAccount)
          .then(accountRepository.saveAccount(targetAccount))
          .then(transactionRepository.saveTransaction(outTransaction))
          .then(transactionRepository.saveTransaction(inTransaction)
          .thenReturn(transactionMapper.toSuccessResponse(
                "Transfer successful. New balance: " + sourceAccount.getBalance(),
                outTransaction.getTransactionId())));
  }

  private Mono<Account> saveAccountAndTransaction(Account account, Transaction transaction) {
    return accountRepository.saveAccount(account)
          .flatMap(savedAccount ->
                transactionRepository.saveTransaction(transaction)
                      .thenReturn(savedAccount));

  }

  private Mono<TransactionResponse> handleError(Throwable ex) {
    log.error("Transaction processing error: {}", ex.getMessage(), ex);

    if (ex instanceof IllegalArgumentException) {
      return Mono.just(transactionMapper.toErrorResponse(404, ex.getMessage()));
    }
    if (ex instanceof IllegalStateException) {
      return Mono.just(transactionMapper.toErrorResponse(400, ex.getMessage()));
    }

    return Mono.just(transactionMapper.toErrorResponse(500,
          "Error processing transaction: " + ex.getMessage()));
  }
}