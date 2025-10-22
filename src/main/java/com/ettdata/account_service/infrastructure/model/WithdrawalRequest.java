package com.ettdata.account_service.infrastructure.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class WithdrawalRequest {
  private String numberAccount;
  private BigDecimal amount;
  private String description;
}
