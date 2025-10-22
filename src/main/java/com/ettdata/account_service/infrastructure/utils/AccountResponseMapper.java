package com.ettdata.account_service.infrastructure.utils;

import com.ettdata.account_service.domain.model.Account;
import com.ettdata.account_service.domain.model.AccountListResponse;
import com.ettdata.account_service.domain.model.AccountResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper component for Account response conversions
 */
@Component
public class AccountResponseMapper {

  // ===== Account Response =====

  /**
   * Creates success response for account creation
   */
  public AccountResponse toSuccessResponse(String message, String accountId) {
    return AccountResponse.builder()
          .codResponse(AccountConstants.HTTP_OK)
          .messageResponse(message)
          .codEntity(accountId)
          .build();
  }

  /**
   * Creates error response
   */
  public AccountResponse toErrorResponse(int code, String message) {
    return AccountResponse.builder()
          .codResponse(code)
          .messageResponse(message)
          .codEntity(null)
          .build();
  }

  /**
   * Converts AccountEntity to success response
   */
  public AccountResponse entityToSuccessResponse(Account entity) {
    return toSuccessResponse(AccountConstants.ACCOUNT_CREATED, entity.getId());
  }

  /**
   * Creates delete success response
   */
  public AccountResponse toDeleteResponse(String accountId) {
    return toSuccessResponse(AccountConstants.CUSTOMER_DELETED, accountId);
  }

  // ===== Account List Response =====

  /**
   * Converts list of AccountEntity to AccountListResponse
   */
  public AccountListResponse toAccountListResponse(List<Account> entities) {
    if (entities == null || entities.isEmpty()) {
      return AccountListResponse.builder()
            .data(Collections.emptyList())
            .Error(null)
            .build();
    }
    return AccountListResponse.builder()
          .data(entities)
          .Error(null)
          .build();
  }

  /**
   * Converts single AccountEntity to AccountListResponse
   */
  public AccountListResponse entityToSingletonResponse(Account entity) {
    if (entity == null) {
      return AccountListResponse.builder()
            .data(Collections.emptyList())
            .Error(null)
            .build();
    }

    return AccountListResponse.builder()
          .data(Collections.singletonList(entity))
          .Error(null)
          .build();
  }
}