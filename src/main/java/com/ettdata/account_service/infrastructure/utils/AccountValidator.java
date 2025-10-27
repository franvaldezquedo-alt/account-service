package com.ettdata.account_service.infrastructure.utils;

import com.ettdata.account_service.domain.model.Account;
import com.ettdata.account_service.domain.model.AccountType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

/**
 * Componente validador para reglas de negocio de cuentas según Proyecto Bancario
 */
@Component
@Slf4j
public class AccountValidator {

  // Límites de movimientos mensuales según tipo de cuenta (Proyecto I)
  private static final int SAVINGS_MAX_MOVEMENTS = 10;           // Ahorro: máximo 10 movimientos/mes
  private static final int FIXED_TERM_MAX_MOVEMENTS = 1;         // Plazo fijo: 1 solo movimiento/mes
  // CURRENT (Corriente): SIN LÍMITE de movimientos

  // Comisiones (Proyecto II)
  private static final int SAVINGS_MAX_FREE_MOVEMENTS = 10;      // Primeros 10 gratis
  private static final int CURRENT_MAX_FREE_MOVEMENTS = 20;      // Primeros 20 gratis
  private static final BigDecimal SAVINGS_MOVEMENT_COMMISSION = new BigDecimal("2.00");
  private static final BigDecimal CURRENT_MOVEMENT_COMMISSION = new BigDecimal("1.50");

  // ==================== VALIDACIONES DE APERTURA ====================

  /**
   * Valida el saldo mínimo de apertura (Proyecto II)
   */
  public Mono<Void> validateMinimumBalance(BigDecimal initialBalance, BigDecimal minimumRequired) {
    if (initialBalance.compareTo(minimumRequired) < 0) {
      log.warn("Initial balance ({}) is less than minimum required ({})",
            initialBalance, minimumRequired);
      return Mono.error(new IllegalArgumentException(
            "Saldo inicial insuficiente. Mínimo requerido: " + minimumRequired));
    }
    return Mono.empty();
  }

  /**
   * Valida si el tipo de cuenta está permitido para el tipo de cliente (Proyecto I)
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
   * Valida el límite de cuentas para clientes PERSONALES (Proyecto I)
   * - Solo puede tener 1 cuenta de ahorro, 1 corriente o cuentas a plazo fijo
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

    if (hasAccountOfType && !"FIXED_TERM".equalsIgnoreCase(requestedAccountType)) {
      log.warn("PERSONAL customer already has an account of type '{}'",
            requestedAccountType);
      return Mono.error(new IllegalStateException(
            AccountConstants.ACCOUNT_ALREADY_EXISTS));
    }

    return Mono.empty();
  }

  /**
   * Valida que clientes EMPRESARIALES no tengan cuentas de ahorro o plazo fijo (Proyecto I)
   */
  public Mono<Void> validateBusinessAccountRestrictions(String customerType, String accountType) {
    if ("BUSINESS".equalsIgnoreCase(customerType)
          && ("SAVINGS".equalsIgnoreCase(accountType) || "FIXED_TERM".equalsIgnoreCase(accountType))) {
      log.warn("Business customer cannot have SAVINGS or FIXED_TERM accounts");
      return Mono.error(new IllegalArgumentException(
            "Clientes empresariales no pueden tener cuentas de ahorro o plazo fijo"));
    }
    return Mono.empty();
  }

  /**
   * Registra validaciones adicionales para clientes VIP y PYME
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

  // ==================== VALIDACIONES DE MOVIMIENTOS (Proyecto I y II) ====================

  /**
   * Valida límite ABSOLUTO de movimientos mensuales (Proyecto I)
   * - SAVINGS: máximo 10 movimientos al mes (BLOQUEA después del límite)
   * - CURRENT: SIN LÍMITE de movimientos
   * - FIXED_TERM: solo 1 movimiento al mes (BLOQUEA después del límite)
   */
  public Mono<Void> validateMovementLimit(Account account) {
    AccountType type = account.getAccountType();
    Integer currentMovements = account.getCantMovements() != null ? account.getCantMovements() : 0;

    switch (type) {
      case SAVINGS:
        if (currentMovements >= SAVINGS_MAX_MOVEMENTS) {
          log.warn("❌ SAVINGS account reached maximum movements: {}/{}",
                currentMovements, SAVINGS_MAX_MOVEMENTS);
          return Mono.error(new IllegalStateException(
                String.format("Cuenta de ahorro alcanzó el límite máximo de %d movimientos mensuales",
                      SAVINGS_MAX_MOVEMENTS)));
        }
        log.info("✅ SAVINGS account movements: {}/{}", currentMovements, SAVINGS_MAX_MOVEMENTS);
        break;

      case CURRENT:
        // ✅ Cuenta corriente NO tiene límite de movimientos
        log.info("✅ CURRENT account has no movement limit (movements: {})", currentMovements);
        break;

      case FIXED_TERM:
        if (currentMovements >= FIXED_TERM_MAX_MOVEMENTS) {
          log.warn("❌ FIXED_TERM account already has {} movement this month", currentMovements);
          return Mono.error(new IllegalStateException(
                "Cuenta a plazo fijo ya realizó su único movimiento mensual permitido"));
        }
        log.info("✅ FIXED_TERM account movements: {}/{}", currentMovements, FIXED_TERM_MAX_MOVEMENTS);
        break;

      default:
        log.warn("Unknown account type: {}", type);
        break;
    }

    return Mono.empty();
  }

  /**
   * Calcula la comisión por movimiento adicional (Proyecto II)
   * Aplica cuando se superan los movimientos gratuitos
   */
  public BigDecimal calculateMovementCommission(Account account) {
    AccountType type = account.getAccountType();
    Integer currentMovements = account.getCantMovements() != null ? account.getCantMovements() : 0;

    switch (type) {
      case SAVINGS:
        // Después de 10 movimientos, NO aplica comisión porque ya se bloqueó
        // Esta comisión se aplicaría si se permitieran movimientos adicionales
        log.debug("SAVINGS account has no commission (blocked at limit)");
        return BigDecimal.ZERO;

      case CURRENT:
        // Cuenta corriente cobra comisión después de 20 movimientos gratuitos
        if (currentMovements >= CURRENT_MAX_FREE_MOVEMENTS) {
          log.info("💰 Applying CURRENT commission of {} for movement #{}",
                CURRENT_MOVEMENT_COMMISSION, currentMovements + 1);
          return CURRENT_MOVEMENT_COMMISSION;
        }
        log.info("✅ CURRENT account still has free movements: {}/{}",
              currentMovements, CURRENT_MAX_FREE_MOVEMENTS);
        break;

      case FIXED_TERM:
        // Plazo fijo no tiene comisión por movimiento
        log.debug("FIXED_TERM account has no movement commission");
        break;

      default:
        log.warn("Unknown account type for commission calculation: {}", type);
        break;
    }

    return BigDecimal.ZERO;
  }

  /**
   * Valida si la cuenta tiene comisión de mantenimiento (Proyecto I)
   * - SAVINGS: libre de comisión
   * - CURRENT: con comisión (excepto PYME que es sin comisión)
   * - FIXED_TERM: libre de comisión
   */
  public boolean hasMaintenanceFee(String customerType, AccountType accountType) {
    if (accountType == AccountType.CURRENT) {
      if ("PYME".equalsIgnoreCase(customerType)) {
        log.info("PYME current account has no maintenance fee");
        return false;
      }
      log.info("Regular current account has maintenance fee");
      return true;
    }

    log.info("Account type {} has no maintenance fee", accountType);
    return false;
  }

  /**
   * Obtiene el número máximo de movimientos según tipo de cuenta
   */
  public int getMaxMovements(AccountType accountType) {
    switch (accountType) {
      case SAVINGS:
        return SAVINGS_MAX_MOVEMENTS;
      case CURRENT:
        return Integer.MAX_VALUE; // Sin límite
      case FIXED_TERM:
        return FIXED_TERM_MAX_MOVEMENTS;
      default:
        return 0;
    }
  }

  /**
   * Obtiene el número máximo de movimientos gratuitos (para comisiones)
   */
  public int getMaxFreeMovements(AccountType accountType) {
    switch (accountType) {
      case SAVINGS:
        return SAVINGS_MAX_FREE_MOVEMENTS;
      case CURRENT:
        return CURRENT_MAX_FREE_MOVEMENTS;
      case FIXED_TERM:
        return FIXED_TERM_MAX_MOVEMENTS;
      default:
        return 0;
    }
  }

  /**
   * Verifica si aún puede realizar movimientos (sin bloqueo)
   */
  public boolean canMakeMovements(Account account) {
    int currentMovements = account.getCantMovements() != null ? account.getCantMovements() : 0;
    int maxMovements = getMaxMovements(account.getAccountType());

    return currentMovements < maxMovements;
  }

  /**
   * Obtiene información del estado de movimientos
   */
  public String getMovementStatus(Account account) {
    int currentMovements = account.getCantMovements() != null ? account.getCantMovements() : 0;
    int maxMovements = getMaxMovements(account.getAccountType());
    int maxFreeMovements = getMaxFreeMovements(account.getAccountType());

    if (account.getAccountType() == AccountType.CURRENT) {
      if (currentMovements < maxFreeMovements) {
        return String.format("Movimientos gratuitos disponibles: %d/%d",
              currentMovements, maxFreeMovements);
      } else {
        return String.format("Movimiento con comisión (movimientos: %d)", currentMovements);
      }
    } else {
      return String.format("Movimientos: %d/%d", currentMovements, maxMovements);
    }
  }

  // ==================== MÉTODOS PRIVADOS ====================

  /**
   * Determina si un tipo de cuenta está permitido para un tipo de cliente
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
        return at.equals("CURRENT");

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