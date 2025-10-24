package com.ettdata.account_service.infrastructure.utils;

import com.ettdata.account_service.domain.model.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Validator component for transaction business rules
 */
@Component
@Slf4j
public class TransactionValidator {

  private static final String INVALID_AMOUNT_MESSAGE = "Amount must be greater than zero";
  private static final String INSUFFICIENT_FUNDS_MESSAGE = "Insufficient funds";
  private static final String SAME_ACCOUNT_MESSAGE = "Source and target accounts must be different";

  /**
   * Validates that the amount is positive
   */
  public Mono<Void> validateAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("Invalid amount: {}", amount);
      return Mono.error(new IllegalArgumentException(INVALID_AMOUNT_MESSAGE));
    }
    return Mono.empty();
  }

  /**
   * Validates that the account has sufficient funds
   */
  public Mono<Void> validateSufficientFunds(Account account, BigDecimal amount) {
    if (account.getBalance().compareTo(amount) < 0) {
      log.warn("Insufficient funds in account {}. Balance: {}, Required: {}",
            account.getAccountNumber(), account.getBalance(), amount);
      return Mono.error(new IllegalStateException(INSUFFICIENT_FUNDS_MESSAGE));
    }
    return Mono.empty();
  }

  /**
   * Validates that source and target accounts are different
   */
  public Mono<Void> validateDifferentAccounts(String sourceAccount, String targetAccount) {
    if (sourceAccount.equals(targetAccount)) {
      log.warn("Attempt to transfer to same account: {}", sourceAccount);
      return Mono.error(new IllegalArgumentException(SAME_ACCOUNT_MESSAGE));
    }
    return Mono.empty();
  }

  /**
   * Válida si una cuenta ha superado su límite de movimientos mensuales
   * según su tipo.
   */
  public Mono<Void> validateMovementCount(Account account) {
    String type = account.getAccountType().name().toUpperCase();
    int currentMovements = account.getCantMovements() == null ? 0 : account.getCantMovements();

    switch (type) {
      case "SAVINGS":
        if (currentMovements >= 10) {
          log.warn("SAVINGS account {} reached the 10-movement monthly limit", account.getAccountNumber());
          return Mono.error(new IllegalStateException("Savings account has reached 10 monthly movements"));
        }
        break;

      case "FIXED_TERM":
        if (currentMovements >= 1) {
          log.warn("FIXED_TERM account {} already performed its single monthly movement", account.getAccountNumber());
          return Mono.error(new IllegalStateException("Fixed-term account only allows one movement per month"));
        }
        break;

      case "CURRENT":
        // Las cuentas corrientes no tienen límite
        log.debug("CURRENT account {} has no movement limit", account.getAccountNumber());
        break;

      default:
        log.warn("Unknown account type: {}", type);
        break;
    }

    return Mono.empty();
  }
}