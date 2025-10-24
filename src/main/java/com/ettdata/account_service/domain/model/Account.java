package com.ettdata.account_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    private String id;
    private String accountNumber;
    private AccountType accountType;
    private String customerId; // Reference to the client
    private List<String> holders; // For business accounts
    private List<String> authorizedSigners;
    private LocalDate openingDate;
    private BigDecimal balance;
    private BigDecimal maintenanceFee;
    private Integer cantMovements;
    private BigDecimal minimumOpeningAmount;
    private AccountStatus accountStatus;
}
