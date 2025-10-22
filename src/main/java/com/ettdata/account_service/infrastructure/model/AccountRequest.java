package com.ettdata.account_service.infrastructure.model;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class AccountRequest {

    @NotBlank(message = "Customer document is required")
    private String customerDocument;

    @NotBlank(message = "Account type is required")
    private String accountType; // SAVINGS, CURRENT, FIXED_TERM

    private List<String> holders; // Optional (for business accounts)

    private List<String> authorizedSigners; // Optional (for business accounts)

    @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance must be zero or greater")
    private BigDecimal initialBalance;

    @DecimalMin(value = "0.0", inclusive = true, message = "Maintenance fee must be zero or greater")
    private BigDecimal maintenanceFee;

    @PositiveOrZero(message = "Movement limit must be zero or greater")
    private Integer movementLimit;

    @DecimalMin(value = "0.0", inclusive = true, message = "Minimum opening amount must be zero or greater")
    private BigDecimal minimumOpeningAmount;
}
