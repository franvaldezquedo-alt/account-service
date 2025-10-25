package com.ettdata.account_service.application.service;

import com.ettdata.account_service.application.port.in.AccountInputPort;
import com.ettdata.account_service.application.port.out.AccountRepositoryOutputPort;
import com.ettdata.account_service.application.port.out.CustomerOutputPort;
import com.ettdata.account_service.domain.error.AccountNotFoundException;
import com.ettdata.account_service.domain.model.Account;
import com.ettdata.account_service.domain.model.AccountListResponse;
import com.ettdata.account_service.domain.model.AccountResponse;
import com.ettdata.account_service.infrastructure.kafka.AccountEventProducer;
import com.ettdata.account_service.infrastructure.model.AccountRequest;
import com.ettdata.account_service.infrastructure.utils.AccountConstants;
import com.ettdata.account_service.infrastructure.utils.AccountMapper;
import com.ettdata.account_service.infrastructure.utils.AccountResponseMapper;
import com.ettdata.account_service.infrastructure.utils.AccountValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.List;

/**
 * Service for bank account management.
 * Implements business rules defined for the banking system.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService implements AccountInputPort {

  private final AccountRepositoryOutputPort accountRepository;
  private final CustomerOutputPort customerClient;
  private final AccountMapper accountMapper;
  private final AccountResponseMapper responseMapper;
  private final AccountValidator validator;
  private final AccountEventProducer accountEventProducer;


  @Override
  public Mono<AccountListResponse> findAllBankAccount() {
    log.info("Retrieving all bank accounts");

    return accountRepository.findAllBankAccount()
          .collectList()
          .map(responseMapper::toAccountListResponse)
          .doOnSuccess(response ->
                log.debug("Found {} accounts", response.getData().size()))
          .doOnError(error ->
                log.error("Error retrieving accounts: {}", error.getMessage()));
  }

  @Override
  public Mono<AccountListResponse> findByIdBankAccount(String id) {
    log.info("Retrieving account by id: {}", id);

    return accountRepository.findByIdBankAccount(id)
          .map(responseMapper::entityToSingletonResponse)
          .switchIfEmpty(Mono.defer(() -> {
            log.warn("Account not found with id: {}", id);
            return Mono.error(new AccountNotFoundException(
                  AccountConstants.BANK_ACCOUNT_NOT_FOUND + id));
          }))
          .doOnSuccess(response -> log.debug("Account found with id: {}", id))
          .doOnError(error ->
                log.error("Error retrieving account {}: {}", id, error.getMessage()));
  }

  @Override
  public Mono<AccountResponse> createBankAccount(AccountRequest request) {
    log.info("Starting account creation for document: {}", request.getCustomerDocument());

    return customerClient.getCustomerByDocument(request.getCustomerDocument())
          .flatMap(customer -> {
            log.info("Customer found - Type: {}, ID: {}",
                  customer.getCustomerType(), customer.getId());

            return validateAccountCreation(request, customer.getCustomerType())
                  .then(accountRepository.findByCustomerId(customer.getId()).collectList())
                  .flatMap(existingAccounts ->
                        processAccountCreation(request, customer.getId(),
                              customer.getCustomerType(), existingAccounts));
          })
          .switchIfEmpty(Mono.defer(() -> {
            log.warn("Customer not found with document: {}", request.getCustomerDocument());
            return Mono.just(responseMapper.toErrorResponse(
                  AccountConstants.HTTP_BAD_REQUEST,
                  AccountConstants.CUSTOMER_NOT_FOUND));
          }))
          .onErrorResume(this::handleError);
  }

  @Override
  public Mono<AccountResponse> deleteByIdBankAccount(String id) {
    log.info("Deleting account with id: {}", id);

    return accountRepository.deleteByIdBankAccount(id)
          .then(Mono.fromCallable(() -> responseMapper.toDeleteResponse(id)))
          .doOnSuccess(response ->
                log.info("Account deleted successfully with id: {}", id))
          .doOnError(error ->
                log.error("Error deleting account {}: {}", id, error.getMessage()));
  }

  // ===== Private Helper Methods =====

  private Mono<Void> validateAccountCreation(AccountRequest request, String customerType) {
    return validator.validateMinimumBalance(
                request.getInitialBalance(),
                request.getMinimumOpeningAmount())
          .then(validator.validateAccountType(
                customerType,
                request.getAccountType()));
  }

  private Mono<AccountResponse> processAccountCreation(AccountRequest request,
                                                       String customerId,
                                                       String customerType,
                                                       List<Account> existingAccounts) {

    return validator.validatePersonalAccountLimit(
                customerType,
                existingAccounts,
                request.getAccountType())
          .then(Mono.defer(() -> {
            validator.logSpecialCustomerRequirements(customerType, request.getAccountType());
            return createAndSaveAccount(request, customerId);
          }));
  }

  private Mono<AccountResponse> createAndSaveAccount(AccountRequest request, String customerId) {
    return Mono.just(request)
          .map(req -> accountMapper.requestToDomain(req, customerId))
          .flatMap(accountRepository::saveAccount)
          .map(responseMapper::entityToSuccessResponse)
          .doOnSuccess(response -> {
            log.info("Account created successfully - ID: {}, Type: {}", response.getCodEntity(), request.getAccountType());

            // 🔹 Enviar evento con customerId
            accountEventProducer.sendAccountCreatedEvent(
                  response.getCodEntity(),
                  request.getInitialBalance(),
                  customerId
            ).subscribe();
          });
  }



  private Mono<AccountResponse> handleError(Throwable ex) {
    log.error("Unexpected error in account operation: {}", ex.getMessage(), ex);

    if (ex instanceof IllegalArgumentException) {
      return Mono.just(responseMapper.toErrorResponse(
            AccountConstants.HTTP_BAD_REQUEST,
            ex.getMessage()));
    }

    if (ex instanceof IllegalStateException) {
      return Mono.just(responseMapper.toErrorResponse(
            AccountConstants.HTTP_BAD_REQUEST,
            ex.getMessage()));
    }

    if (ex instanceof AccountNotFoundException) {
      return Mono.just(responseMapper.toErrorResponse(
            AccountConstants.HTTP_NOT_FOUND,
            ex.getMessage()));
    }

    return Mono.just(responseMapper.toErrorResponse(
          AccountConstants.HTTP_INTERNAL_ERROR,
          "Error processing request: " + ex.getMessage()));
  }
}