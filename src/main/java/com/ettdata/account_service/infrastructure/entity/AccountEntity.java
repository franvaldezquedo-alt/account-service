package com.ettdata.account_service.infrastructure.entity;

import com.ettdata.account_service.domain.model.AccountStatus;
import com.ettdata.account_service.domain.model.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Document(collection = "accounts")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEntity {
    @Id
    private String id;
    private String accountNumber;                // Unique account number
    private AccountType accountType;             // SAVINGS, CURRENT, FIXED_TERM
    private String customerId;                   // Reference to the customer
    private List<String> holders;                // For business accounts
    private List<String> authorizedSigners;      // Authorized signers
    private LocalDate openingDate;               // Date of creation
    private BigDecimal balance;                  // Current balance
    private BigDecimal maintenanceFee;           // Monthly maintenance fee (if applicable)
    private Integer cantMovements;               // Free transactions per month
    private BigDecimal minimumOpeningAmount;     // Minimum amount to open the account
    private AccountStatus accountStatus;         // ACTIVE, INACTIVE, BLOCKED
}
