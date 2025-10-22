package com.ettdata.account_service.infrastructure.utils;

import com.ettdata.account_service.domain.model.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validator component for account business rules
 */
@Component
@Slf4j
public class AccountValidator {

  /**
   * Validates minimum opening balance
   */
  public Mono<Void> validateMinimumBalance(BigDecimal initialBalance, BigDecimal minimumRequired) {
    if (initialBalance.compareTo(minimumRequired) < 0) {
      log.warn("Initial balance ({}) is less than minimum required ({})",
            initialBalance, minimumRequired);
      return Mono.error(new IllegalArgumentException(
            AccountConstants.ACCOUNT_MIN_BALANCE_ERROR));
    }
    return Mono.empty();
  }

  /**
   * Validates if account type is allowed for customer type
   */
  public Mono<Void> validateAccountType(String customerType, String accountType) {
    if (!isAccountTypeAllowed(customerType, accountType)) {
      log.warn("Account type '{}' not allowed for customer type '{}'",
            accountType, customerType);
      return Mono.error(new IllegalArgumentException(
            AccountConstants.ACCOUNT_TYPE_NOT_ALLOWED));
    }
    return Mono.empty();
  }

  /**
   * Validates account limit for PERSONAL customers
   */
  public Mono<Void> validatePersonalAccountLimit(String customerType,
                                                 List<Account> existingAccounts,
                                                 String requestedAccountType) {
    if (!"PERSONAL".equalsIgnoreCase(customerType)) {
      return Mono.empty();
    }

    boolean hasAccountOfType = existingAccounts.stream()
          .anyMatch(acc -> acc.getAccountType().name()
                .equalsIgnoreCase(requestedAccountType));

    if (hasAccountOfType) {
      log.warn("PERSONAL customer already has an account of type '{}'",
            requestedAccountType);
      return Mono.error(new IllegalStateException(
            AccountConstants.ACCOUNT_ALREADY_EXISTS));
    }

    return Mono.empty();
  }

  /**
   * Logs additional validations for VIP and PYME customers
   */
  public void logSpecialCustomerRequirements(String customerType, String accountType) {
    if ("VIP".equalsIgnoreCase(customerType)
          && "SAVINGS".equalsIgnoreCase(accountType)) {
      log.info("VIP customer requesting savings account - validate credit card requirement");
    }

    if ("PYME".equalsIgnoreCase(customerType)
          && "CURRENT".equalsIgnoreCase(accountType)) {
      log.info("PYME customer requesting current account - validate commission-free benefit");
    }
  }

  // ===== Private Helper Methods =====

  /**
   * Determines if an account type is allowed for a customer type
   */
  private boolean isAccountTypeAllowed(String customerType, String accountType) {
    if (customerType == null || accountType == null) {
      log.warn("Null parameters in account type validation");
      return false;
    }

    String ct = customerType.toUpperCase();
    String at = accountType.toUpperCase();

    switch (ct) {
      case "PERSONAL":
        return at.equals("SAVINGS") || at.equals("CURRENT") || at.equals("FIXED_TERM");
      case "BUSINESS":
      case "PYME":
        return at.equals("CURRENT");
      case "VIP":
        return at.equals("SAVINGS") || at.equals("CURRENT");
      default:
        log.warn("Unknown customer type: {}", customerType);
        return false;
    }
  }
}