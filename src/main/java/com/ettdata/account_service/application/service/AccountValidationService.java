package com.ettdata.account_service.application.service;

import com.ettdata.account_service.application.port.in.TransactionValidationInput;
import com.ettdata.account_service.application.port.out.AccountRepositoryOutputPort;
import com.ettdata.account_service.domain.model.Account;
import com.ettdata.account_service.infrastructure.utils.AccountValidator;
import com.ettdata.avro.AccountValidationRequest;
import com.ettdata.avro.AccountValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Servicio de validación de transacciones bancarias
 * Maneja depósitos, retiros y transferencias con validaciones de límites y comisiones
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountValidationService implements TransactionValidationInput {

  private final AccountRepositoryOutputPort accountRepository;
  private final AccountValidator accountValidator;

  // ==================== OPERACIONES PRINCIPALES ====================

  /**
   * Procesa y valida una solicitud de retiro
   */
  @Override
  public Mono<AccountValidationResponse> retiro(AccountValidationRequest request) {
    log.info("🔄 Procesando validación de retiro para transactionId={}", request.getTransactionId());

    return validateRequest(request)
          .flatMap(validRequest ->
                accountRepository.findByNumberAccount(String.valueOf(validRequest.getAccountNumber()))
                      .flatMap(account -> validateAndProcessWithdraw(validRequest, account))
                      .switchIfEmpty(buildAccountNotFoundResponse(validRequest))
          )
          .onErrorResume(error -> buildErrorResponse(request, error));
  }

  /**
   * Procesa y valida una solicitud de depósito
   */
  @Override
  public Mono<AccountValidationResponse> deposito(AccountValidationRequest request) {
    log.info("🔄 Procesando validación de depósito para transactionId={}", request.getTransactionId());

    return validateRequest(request)
          .flatMap(validRequest ->
                accountRepository.findByNumberAccount(String.valueOf(validRequest.getAccountNumber()))
                      .flatMap(account -> validateAndProcessDeposit(validRequest, account))
                      .switchIfEmpty(buildAccountNotFoundResponse(validRequest))
          )
          .onErrorResume(error -> buildErrorResponse(request, error));
  }

  /**
   * Procesa y valida una solicitud de transferencia
   */
  @Override
  public Mono<AccountValidationResponse> transferencia(AccountValidationRequest request) {
    log.info("🔄 Procesando validación de transferencia para transactionId={}", request.getTransactionId());

    if (request.getTargetAccountNumber() == null || request.getTargetAccountNumber().isBlank()) {
      return Mono.just(buildTargetMissingResponse(request));
    }

    return validateRequest(request)
          .flatMap(validRequest ->
                accountRepository.findByNumberAccount(String.valueOf(validRequest.getAccountNumber()))
                      .flatMap(account -> validateAndProcessTransfer(validRequest, account))
                      .switchIfEmpty(buildAccountNotFoundResponse(validRequest))
          )
          .onErrorResume(error -> buildErrorResponse(request, error));
  }

  // ==================== VALIDACIONES GENERALES ====================

  /**
   * Valida campos básicos de la solicitud
   */
  private Mono<AccountValidationRequest> validateRequest(AccountValidationRequest request) {
    if (request.getAmount() <= 0) {
      return Mono.error(new IllegalArgumentException("El monto debe ser mayor a cero"));
    }

    if (request.getAccountNumber() == null || request.getAccountNumber().isBlank()) {
      return Mono.error(new IllegalArgumentException("Número de cuenta inválido"));
    }

    return Mono.just(request);
  }

  /**
   * Verifica si la cuenta tiene fondos suficientes
   */
  private boolean hasInsufficientFunds(Account account, BigDecimal amount) {
    return account.getBalance().compareTo(amount) < 0;
  }

  // ==================== RETIRO ====================

  /**
   * Valida y procesa un retiro
   * - Valida límite de movimientos según tipo de cuenta
   * - Calcula comisión si aplica (solo CURRENT después de 20 movimientos)
   * - Valida fondos suficientes
   */
  private Mono<AccountValidationResponse> validateAndProcessWithdraw(
        AccountValidationRequest request,
        Account account) {

    log.info("🔍 Cuenta encontrada: {}, saldo: {}, {}",
          account.getAccountNumber(),
          account.getBalance(),
          accountValidator.getMovementStatus(account));

    BigDecimal requestAmount = BigDecimal.valueOf(request.getAmount());

    // 1. Validar límite de movimientos (BLOQUEA si se excede para SAVINGS y FIXED_TERM)
    return accountValidator.validateMovementLimit(account)
          .then(Mono.defer(() -> {
            // 2. Calcular comisión si aplica (solo CURRENT)
            BigDecimal commission = accountValidator.calculateMovementCommission(account);
            BigDecimal totalAmount = requestAmount.add(commission);

            if (commission.compareTo(BigDecimal.ZERO) > 0) {
              log.info("💰 Retiro con comisión: monto={}, comisión={}, total={}",
                    requestAmount, commission, totalAmount);
            } else {
              log.info("💰 Retiro sin comisión: monto={}", requestAmount);
            }

            // 3. Validar fondos suficientes (incluyendo comisión)
            if (hasInsufficientFunds(account, totalAmount)) {
              log.warn("⚠️ Fondos insuficientes: saldo={}, total requerido={}",
                    account.getBalance(), totalAmount);
              return Mono.just(buildInsufficientFundsResponse(request, commission));
            }

            // 4. Procesar retiro
            return processWithdraw(request, account, requestAmount, commission);
          }))
          .onErrorResume(error -> {
            log.error("❌ Error en validación: {}", error.getMessage());
            return Mono.just(buildValidationErrorResponse(request, error.getMessage()));
          });
  }

  /**
   * Ejecuta el retiro y actualiza el saldo y contador de movimientos
   */
  private Mono<AccountValidationResponse> processWithdraw(
        AccountValidationRequest request,
        Account account,
        BigDecimal amount,
        BigDecimal commission) {

    BigDecimal totalDeducted = amount.add(commission);
    account.setBalance(account.getBalance().subtract(totalDeducted));
    account.setCantMovements(account.getCantMovements() + 1);

    return accountRepository.saveOrUpdateAccount(account)
          .map(updated -> {
            log.info("✅ Retiro aplicado: cuenta={}, nuevo saldo={}, movimientos={}, comisión={}",
                  updated.getAccountNumber(),
                  updated.getBalance(),
                  updated.getCantMovements(),
                  commission);
            return buildWithdrawSuccessResponse(request, commission);
          });
  }

  // ==================== DEPÓSITO ====================

  /**
   * Valida y procesa un depósito
   * - Valida límite de movimientos según tipo de cuenta
   * - Calcula comisión si aplica (solo CURRENT después de 20 movimientos)
   */
  private Mono<AccountValidationResponse> validateAndProcessDeposit(
        AccountValidationRequest request,
        Account account) {

    log.info("🔍 Cuenta encontrada para depósito: {}, {}",
          account.getAccountNumber(),
          accountValidator.getMovementStatus(account));

    // 1. Validar límite de movimientos (BLOQUEA si se excede para SAVINGS y FIXED_TERM)
    return accountValidator.validateMovementLimit(account)
          .then(Mono.defer(() -> {
            // 2. Calcular comisión si aplica (solo CURRENT)
            BigDecimal commission = accountValidator.calculateMovementCommission(account);
            BigDecimal amount = BigDecimal.valueOf(request.getAmount());
            BigDecimal netDeposit = amount.subtract(commission);

            if (commission.compareTo(BigDecimal.ZERO) > 0) {
              log.info("💰 Depósito con comisión: monto={}, comisión={}, neto={}",
                    amount, commission, netDeposit);
            } else {
              log.info("💰 Depósito sin comisión: monto={}", amount);
            }

            // 3. Procesar depósito
            return processDeposit(request, account, amount, commission, netDeposit);
          }))
          .onErrorResume(error -> {
            log.error("❌ Error en validación de depósito: {}", error.getMessage());
            return Mono.just(buildValidationErrorResponse(request, error.getMessage()));
          });
  }

  /**
   * Ejecuta el depósito y actualiza el saldo y contador de movimientos
   */
  private Mono<AccountValidationResponse> processDeposit(
        AccountValidationRequest request,
        Account account,
        BigDecimal amount,
        BigDecimal commission,
        BigDecimal netDeposit) {

    account.setBalance(account.getBalance().add(netDeposit));
    account.setCantMovements(account.getCantMovements() + 1);

    return accountRepository.saveOrUpdateAccount(account)
          .map(updated -> {
            log.info("✅ Depósito aplicado: cuenta={}, nuevo saldo={}, movimientos={}, comisión={}",
                  updated.getAccountNumber(),
                  updated.getBalance(),
                  updated.getCantMovements(),
                  commission);
            return buildDepositSuccessResponse(request, commission);
          });
  }

  // ==================== TRANSFERENCIA ====================

  /**
   * Valida y procesa una transferencia entre cuentas
   * - Valida límite de movimientos de cuenta origen
   * - Calcula comisión si aplica (solo CURRENT)
   * - Valida fondos suficientes
   * - Valida que exista cuenta destino
   */
  private Mono<AccountValidationResponse> validateAndProcessTransfer(
        AccountValidationRequest request,
        Account sourceAccount) {

    BigDecimal requestAmount = BigDecimal.valueOf(request.getAmount());

    // 1. Validar límite de movimientos de cuenta origen
    return accountValidator.validateMovementLimit(sourceAccount)
          .then(Mono.defer(() -> {
            // 2. Calcular comisión
            BigDecimal commission = accountValidator.calculateMovementCommission(sourceAccount);
            BigDecimal totalAmount = requestAmount.add(commission);

            if (commission.compareTo(BigDecimal.ZERO) > 0) {
              log.info("💰 Transferencia con comisión: monto={}, comisión={}, total={}",
                    requestAmount, commission, totalAmount);
            } else {
              log.info("💰 Transferencia sin comisión: monto={}", requestAmount);
            }

            // 3. Validar fondos suficientes
            if (hasInsufficientFunds(sourceAccount, totalAmount)) {
              log.warn("⚠️ Fondos insuficientes para transferencia: saldo={}, total requerido={}",
                    sourceAccount.getBalance(), totalAmount);
              return Mono.just(buildInsufficientFundsResponse(request, commission));
            }

            // 4. Buscar cuenta destino y procesar
            return accountRepository.findByNumberAccount(request.getTargetAccountNumber().toString())
                  .flatMap(targetAccount ->
                        processTransfer(request, sourceAccount, targetAccount, requestAmount, commission)
                  )
                  .switchIfEmpty(Mono.just(buildTargetMissingResponse(request)));
          }))
          .onErrorResume(error -> {
            log.error("❌ Error en validación de transferencia: {}", error.getMessage());
            return Mono.just(buildValidationErrorResponse(request, error.getMessage()));
          });
  }

  /**
   * Ejecuta la transferencia actualizando ambas cuentas
   */
  private Mono<AccountValidationResponse> processTransfer(
        AccountValidationRequest request,
        Account sourceAccount,
        Account targetAccount,
        BigDecimal amount,
        BigDecimal commission) {

    // Actualizar cuenta origen (se descuenta monto + comisión)
    sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount).subtract(commission));
    sourceAccount.setCantMovements(sourceAccount.getCantMovements() + 1);

    // Actualizar cuenta destino (se acredita solo el monto, sin comisión)
    targetAccount.setBalance(targetAccount.getBalance().add(amount));
    targetAccount.setCantMovements(targetAccount.getCantMovements() + 1);

    // Guardar ambas cuentas
    return accountRepository.saveOrUpdateAccount(sourceAccount)
          .then(accountRepository.saveOrUpdateAccount(targetAccount))
          .thenReturn(buildTransferSuccessResponse(request, commission))
          .doOnSuccess(response ->
                log.info("✅ Transferencia completada: origen={} (saldo={}), destino={} (saldo={}), monto={}, comisión={}",
                      sourceAccount.getAccountNumber(),
                      sourceAccount.getBalance(),
                      targetAccount.getAccountNumber(),
                      targetAccount.getBalance(),
                      amount,
                      commission)
          );
  }

  // ==================== RESPONSE BUILDERS ====================

  /**
   * Construye respuesta exitosa de retiro
   */
  private AccountValidationResponse buildWithdrawSuccessResponse(
        AccountValidationRequest request,
        BigDecimal commission) {
    String message = commission.compareTo(BigDecimal.ZERO) > 0
          ? String.format("Retiro registrado. Comisión aplicada: S/ %.2f", commission)
          : "Retiro registrado correctamente";

    return AccountValidationResponse.newBuilder()
          .setTransactionId(request.getTransactionId())
          .setAccountNumber(request.getAccountNumber())
          .setCodResponse(200)
          .setMessageResponse(message)
          .build();
  }

  /**
   * Construye respuesta exitosa de depósito
   */
  private AccountValidationResponse buildDepositSuccessResponse(
        AccountValidationRequest request,
        BigDecimal commission) {
    String message = commission.compareTo(BigDecimal.ZERO) > 0
          ? String.format("Depósito registrado. Comisión descontada: S/ %.2f", commission)
          : "Depósito registrado correctamente";

    return AccountValidationResponse.newBuilder()
          .setTransactionId(request.getTransactionId())
          .setAccountNumber(request.getAccountNumber())
          .setCodResponse(200)
          .setMessageResponse(message)
          .build();
  }

  /**
   * Construye respuesta exitosa de transferencia
   */
  private AccountValidationResponse buildTransferSuccessResponse(
        AccountValidationRequest request,
        BigDecimal commission) {
    String message = commission.compareTo(BigDecimal.ZERO) > 0
          ? String.format("Transferencia registrada. Comisión aplicada: S/ %.2f", commission)
          : "Transferencia registrada correctamente";

    return AccountValidationResponse.newBuilder()
          .setTransactionId(request.getTransactionId())
          .setAccountNumber(request.getAccountNumber())
          .setCodResponse(200)
          .setMessageResponse(message)
          .build();
  }

  /**
   * Construye respuesta de fondos insuficientes
   */
  private AccountValidationResponse buildInsufficientFundsResponse(
        AccountValidationRequest request,
        BigDecimal commission) {
    String message = commission.compareTo(BigDecimal.ZERO) > 0
          ? String.format("Fondos insuficientes (incluye comisión de S/ %.2f)", commission)
          : "Fondos insuficientes";

    return AccountValidationResponse.newBuilder()
          .setTransactionId(request.getTransactionId())
          .setAccountNumber(request.getAccountNumber())
          .setCodResponse(400)
          .setMessageResponse(message)
          .build();
  }

  /**
   * Construye respuesta de cuenta no encontrada
   */
  private Mono<AccountValidationResponse> buildAccountNotFoundResponse(AccountValidationRequest request) {
    log.error("❌ Cuenta no encontrada: {}", request.getAccountNumber());

    return Mono.just(AccountValidationResponse.newBuilder()
          .setTransactionId(request.getTransactionId())
          .setAccountNumber(request.getAccountNumber())
          .setCodResponse(404)
          .setMessageResponse("Cuenta no encontrada")
          .build());
  }

  /**
   * Construye respuesta de error interno
   */
  private Mono<AccountValidationResponse> buildErrorResponse(
        AccountValidationRequest request,
        Throwable error) {

    log.error("💥 Error procesando transacción: {}", error.getMessage(), error);

    return Mono.just(AccountValidationResponse.newBuilder()
          .setTransactionId(request.getTransactionId())
          .setAccountNumber(request.getAccountNumber())
          .setCodResponse(500)
          .setMessageResponse("Error interno del servidor: " + error.getMessage())
          .build());
  }

  /**
   * Construye respuesta de cuenta destino inválida
   */
  private AccountValidationResponse buildTargetMissingResponse(AccountValidationRequest request) {
    return AccountValidationResponse.newBuilder()
          .setTransactionId(request.getTransactionId())
          .setAccountNumber(request.getAccountNumber())
          .setCodResponse(400)
          .setMessageResponse("Cuenta destino inválida o no encontrada")
          .build();
  }

  /**
   * Construye respuesta de error de validación
   */
  private AccountValidationResponse buildValidationErrorResponse(
        AccountValidationRequest request,
        String errorMessage) {
    return AccountValidationResponse.newBuilder()
          .setTransactionId(request.getTransactionId())
          .setAccountNumber(request.getAccountNumber())
          .setCodResponse(400)
          .setMessageResponse(errorMessage)
          .build();
  }
}