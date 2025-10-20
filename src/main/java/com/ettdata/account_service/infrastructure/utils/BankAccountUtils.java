package com.ettdata.account_service.infrastructure.utils;

import com.ettdata.account_service.domain.model.*;
import com.ettdata.account_service.infrastructure.entity.BankAccountEntity;
import com.ettdata.account_service.infrastructure.model.BankAccountRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BankAccountUtils {

    public static BankAccount convertRequestToEntity(BankAccountRequest request, String customerId) {
        return BankAccount.builder()
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
    public static BankAccountResponse convertEntityToResponse(BankAccount account) {
        return createSuccessResponse(BanjAccountConstants.ACCOUNT_CREATED, account.getId());
    }

    /** Crea respuesta de éxito genérica */
    private static BankAccountResponse createSuccessResponse(String message, String entityId) {
        return BankAccountResponse.builder()
                .codResponse(BanjAccountConstants.HTTP_OK)
                .messageResponse(message)
                .codEntity(entityId)
                .build();
    }

    /** Crea respuesta de error genérica */
    public static BankAccountResponse createErrorResponse(int code, String message) {
        return BankAccountResponse.builder()
                .codResponse(code)
                .messageResponse(message)
                .codEntity(null)
                .build();
    }


    public static BankAccountEntity convertDomainToEntity(BankAccount domain) {
        if (domain == null) return null;

        return BankAccountEntity.builder()
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

    public static BankAccount convertEntityToDomain(BankAccountEntity entity) {
        if (entity == null) return null;

        return BankAccount.builder()
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

    public static BankAccountListResponse converBankAccountListResponse(List<BankAccountEntity> entities) {
        List<BankAccount> accounts = new ArrayList<>();
        for (BankAccountEntity entity : entities) {
            accounts.add(convertEntityToDomain(entity));
        }
        return BankAccountListResponse.builder()
                .data(accounts)
                .Error(null)
                .build();

    }


}
