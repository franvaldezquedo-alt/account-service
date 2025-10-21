package com.ettdata.account_service.infrastructure.utils;

import com.ettdata.account_service.domain.model.*;
import com.ettdata.account_service.infrastructure.entity.AccountEntity;
import com.ettdata.account_service.infrastructure.model.AccountRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountUtils {

    public static Account convertRequestToEntity(AccountRequest request, String customerId) {
        return Account.builder()
                .accountNumber("ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .accountType(AccountType.valueOf(request.getAccountType()))
                .customerId(customerId)
                .holders(request.getHolders())
                .authorizedSigners(request.getAuthorizedSigners())
                .openingDate(LocalDate.now())
                .balance(request.getInitialBalance())
                .maintenanceFee(request.getMaintenanceFee())
                .movementLimit(request.getMovementLimit())
                .minimumOpeningAmount(request.getMinimumOpeningAmount())
                .accountStatus(AccountStatus.ACTIVE)
                .transactionList(new ArrayList<>())
                .build();
    }

    /** Crea respuesta exitosa */
    public static AccountResponse convertEntityToResponse(Account account) {
        return createSuccessResponse(AccountConstants.ACCOUNT_CREATED, account.getId());
    }

    /** Crea respuesta de éxito genérica */
    private static AccountResponse createSuccessResponse(String message, String entityId) {
        return AccountResponse.builder()
                .codResponse(AccountConstants.HTTP_OK)
                .messageResponse(message)
                .codEntity(entityId)
                .build();
    }

    /** Crea respuesta de error genérica */
    public static AccountResponse createErrorResponse(int code, String message) {
        return AccountResponse.builder()
                .codResponse(code)
                .messageResponse(message)
                .codEntity(null)
                .build();
    }


    public static AccountEntity convertDomainToEntity(Account domain) {
        if (domain == null) return null;

        return AccountEntity.builder()
                .id(domain.getId())
                .accountNumber(domain.getAccountNumber())
                .accountType(domain.getAccountType())
                .customerId(domain.getCustomerId())
                .holders(domain.getHolders())
                .authorizedSigners(domain.getAuthorizedSigners())
                .openingDate(domain.getOpeningDate())
                .balance(domain.getBalance())
                .maintenanceFee(domain.getMaintenanceFee())
                .movementLimit(domain.getMovementLimit())
                .minimumOpeningAmount(domain.getMinimumOpeningAmount())
                .accountStatus(domain.getAccountStatus())
                .transactionList(domain.getTransactionList())
                .build();
    }

    public static Account convertEntityToDomain(AccountEntity entity) {
        if (entity == null) return null;

        return Account.builder()
                .id(entity.getId())
                .accountNumber(entity.getAccountNumber())
                .accountType(entity.getAccountType())
                .customerId(entity.getCustomerId())
                .holders(entity.getHolders())
                .authorizedSigners(entity.getAuthorizedSigners())
                .openingDate(entity.getOpeningDate())
                .balance(entity.getBalance())
                .maintenanceFee(entity.getMaintenanceFee())
                .movementLimit(entity.getMovementLimit())
                .minimumOpeningAmount(entity.getMinimumOpeningAmount())
                .accountStatus(entity.getAccountStatus())
                .transactionList(entity.getTransactionList())
                .build();
    }

    public static AccountListResponse converBankAccountListResponse(List<AccountEntity> entities) {
        List<Account> accounts = new ArrayList<>();
        for (AccountEntity entity : entities) {
            accounts.add(convertEntityToDomain(entity));
        }
        return AccountListResponse.builder()
                .data(accounts)
                .Error(null)
                .build();

    }

    public static AccountListResponse ConvertBackAccountSingletonResponse(AccountEntity bankAccount){
       return AccountListResponse.builder()
                .data(List.of(convertEntityToDomain(bankAccount)))
                .Error(null)
                .build();

    }

    /**
     * Crea respuesta para operación de eliminación
     */
    public static AccountResponse convertBankAccountResponseDelete(String bankAccountId) {
        return createSuccessResponse(AccountConstants.CUSTOMER_DELETED, bankAccountId);
    }


}
