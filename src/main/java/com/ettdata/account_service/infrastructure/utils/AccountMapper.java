package com.ettdata.account_service.infrastructure.utils;

import com.ettdata.account_service.domain.model.Account;
import com.ettdata.account_service.domain.model.AccountStatus;
import com.ettdata.account_service.domain.model.AccountType;
import com.ettdata.account_service.infrastructure.entity.AccountEntity;
import com.ettdata.account_service.infrastructure.model.AccountRequest;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.UUID;


/**
 * Mapper component for Account conversions between layers
 */
@Component
public class AccountMapper {

  private final TransactionMapper transactionMapper;

  public AccountMapper(TransactionMapper transactionMapper) {
    this.transactionMapper = transactionMapper;
  }

  // ===== Entity to Domain =====

  /**
   * Converts AccountEntity to Account domain model usado en el mapper
   */
  public Account toDomain(AccountEntity entity) {
    if (entity == null) {
      return null;
    }

    return Account.builder()
          .id(entity.getId())
          .accountNumber(entity.getAccountNumber())
          .accountType(entity.getAccountType())
          .balance(entity.getBalance())
          .accountStatus(entity.getAccountStatus())
          .customerId(entity.getCustomerId())
          .holders(entity.getHolders())
          .authorizedSigners(entity.getAuthorizedSigners())
          .openingDate(entity.getOpeningDate())
          .maintenanceFee(entity.getMaintenanceFee())
          .cantMovements(entity.getCantMovements())
          .minimumOpeningAmount(entity.getMinimumOpeningAmount())
          .build();
  }


  // ===== Domain to Entity =====

  /**
   * Converts Account domain model to AccountEntity
   */
  public AccountEntity toEntity(Account domain) {
    if (domain == null) {
      return null;
    }

    return AccountEntity.builder()
          .id(domain.getId())
          .accountNumber(domain.getAccountNumber())
          .accountType(domain.getAccountType())
          .balance(domain.getBalance())
          .accountStatus(domain.getAccountStatus())
          .customerId(domain.getCustomerId())
          .holders(domain.getHolders())
          .authorizedSigners(domain.getAuthorizedSigners())
          .openingDate(domain.getOpeningDate())
          .maintenanceFee(domain.getMaintenanceFee())
          .cantMovements(domain.getCantMovements())
          .minimumOpeningAmount(domain.getMinimumOpeningAmount())
          .build();
  }

  // ===== Request to Entity =====

  /**
   * Converts AccountRequest to Account with generated account number
   */
  public Account requestToDomain(AccountRequest request, String customerId) {
    if (request == null) {
      return null;
    }

    return Account.builder()
          .accountNumber(generateAccountNumber())
          .accountType(AccountType.valueOf(request.getAccountType().toUpperCase()))
          .customerId(customerId)
          .holders(request.getHolders())
          .authorizedSigners(request.getAuthorizedSigners())
          .openingDate(LocalDate.now())
          .balance(request.getInitialBalance())
          .maintenanceFee(request.getMaintenanceFee())
          .cantMovements(0)
          .minimumOpeningAmount(request.getMinimumOpeningAmount())
          .accountStatus(AccountStatus.ACTIVE)
          .build();
  }

  // ===== Private Helpers =====

  /**
   * Generates unique account number
   */
  private String generateAccountNumber() {
    return "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }
}