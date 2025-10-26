package com.ettdata.account_service.application.service;

import com.ettdata.account_service.application.port.in.TransactionValidationInput;
import com.ettdata.account_service.application.port.out.AccountRepositoryOutputPort;
import com.ettdata.account_service.domain.model.Account;
import com.ettdata.avro.AccountValidationRequest;
import com.ettdata.avro.AccountValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountValidationService implements TransactionValidationInput {

    private final AccountRepositoryOutputPort accountRepository;

    @Override
    public Mono<AccountValidationResponse> retiro(AccountValidationRequest request) {
        log.info("🔄 Procesando validación para transactionId={}", request.getTransactionId());

        return validateRequest(request)
                .flatMap(validRequest ->
                        accountRepository.findByNumberAccount(String.valueOf(validRequest.getAccountNumber()))
                                .flatMap(account -> validateAndProcessWithdraw(validRequest, account))
                                .switchIfEmpty(buildAccountNotFoundResponse(validRequest))
                )
                .onErrorResume(error -> buildErrorResponse(request, error));
    }

    @Override
    public Mono<AccountValidationResponse> deposito(AccountValidationRequest request) {
        log.info("🔄 Procesando validación de depósito para transactionId={}", request.getTransactionId());

        return validateRequest(request)
                .flatMap(validRequest ->
                        accountRepository.findByNumberAccount(String.valueOf(validRequest.getAccountNumber()))
                                .flatMap(account -> processDeposit(validRequest, account))
                                .switchIfEmpty(buildAccountNotFoundResponse(validRequest))
                )
                .onErrorResume(error -> buildErrorResponse(request, error));
    }

    @Override
    public Mono<AccountValidationResponse> transferencia(AccountValidationRequest request) {
        log.info("🔄 Procesando validación de transferencia para transactionId={}", request.getTransactionId());

        // Validar que exista cuenta destino en la solicitud
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

    private Mono<AccountValidationRequest> validateRequest(AccountValidationRequest request) {
        if (request.getAmount() <= 0) {
            return Mono.error(new IllegalArgumentException("El monto debe ser mayor a cero"));
        }

        if (request.getAccountNumber() == null || request.getAccountNumber().isBlank()) {
            return Mono.error(new IllegalArgumentException("Número de cuenta inválido"));
        }

        return Mono.just(request);
    }

    private Mono<AccountValidationResponse> validateAndProcessWithdraw(
            AccountValidationRequest request,
            Account account) {

        log.info("🔍 Cuenta encontrada: {}, saldo actual: {}",
                account.getAccountNumber(), account.getBalance());

        BigDecimal requestAmount = BigDecimal.valueOf(request.getAmount());

        if (hasInsufficientFunds(account, requestAmount)) {
            log.warn("⚠️ Fondos insuficientes: cuenta={}, saldo={}, monto solicitado={}",
                    account.getAccountNumber(), account.getBalance(), requestAmount);
            return Mono.just(buildInsufficientFundsResponse(request));
        }

        return processWithdraw(request, account, requestAmount);
    }

    private Mono<AccountValidationResponse> validateAndProcessTransfer(
            AccountValidationRequest request,
            Account sourceAccount) {

        BigDecimal requestAmount = BigDecimal.valueOf(request.getAmount());

        if (hasInsufficientFunds(sourceAccount, requestAmount)) {
            return Mono.just(buildInsufficientFundsResponse(request));
        }

        // Buscar cuenta destino
        return accountRepository.findByNumberAccount(request.getTargetAccountNumber().toString())
                .flatMap(targetAccount -> {
                    // Actualizar saldos
                    sourceAccount.setBalance(sourceAccount.getBalance().subtract(requestAmount));
                    sourceAccount.setCantMovements(sourceAccount.getCantMovements()+1);
                    targetAccount.setBalance(targetAccount.getBalance().add(requestAmount));
                    targetAccount.setCantMovements(targetAccount.getCantMovements() + 1);

                    // Guardar ambas cuentas
                    return accountRepository.saveOrUpdateAccount(sourceAccount)
                            .then(accountRepository.saveOrUpdateAccount(targetAccount))
                            .thenReturn(buildTransferSuccessResponse(request));
                })
                .switchIfEmpty(Mono.just(buildTargetMissingResponse(request)));
    }


    private Mono<AccountValidationResponse> processWithdraw(
            AccountValidationRequest request,
            Account account,
            BigDecimal amount) {

        account.setBalance(account.getBalance().subtract(amount));
        account.setCantMovements(account.getCantMovements() + 1);

        return accountRepository.saveOrUpdateAccount(account)
                .map(updated -> {
                    log.info("✅ Retiro aplicado correctamente: cuenta={}, nuevo saldo={}",
                            updated.getAccountNumber(), updated.getBalance());
                    return buildWithdrawSuccessResponse(request);
                });
    }

    private Mono<AccountValidationResponse> processDeposit(
            AccountValidationRequest request,
            Account account) {

        BigDecimal amount = BigDecimal.valueOf(request.getAmount());
        account.setBalance(account.getBalance().add(amount));
        account.setCantMovements(account.getCantMovements() + 1);

        return accountRepository.saveOrUpdateAccount(account)
                .map(updated -> {
                    log.info("✅ Depósito aplicado correctamente: cuenta={}, nuevo saldo={}",
                            updated.getAccountNumber(), updated.getBalance());
                    return buildDepositSuccessResponse(request);
                });
    }

    private boolean hasInsufficientFunds(Account account, BigDecimal amount) {
        return account.getBalance().compareTo(amount) < 0;
    }

    // ========== Response Builders ==========

    private AccountValidationResponse buildWithdrawSuccessResponse(AccountValidationRequest request) {
        return AccountValidationResponse.newBuilder()
                .setTransactionId(request.getTransactionId())
                .setAccountNumber(request.getAccountNumber())
                .setCodResponse(200)
                .setMessageResponse("Retiro registrado correctamente")
                .build();
    }

    private AccountValidationResponse buildDepositSuccessResponse(AccountValidationRequest request) {
        return AccountValidationResponse.newBuilder()
                .setTransactionId(request.getTransactionId())
                .setAccountNumber(request.getAccountNumber())
                .setCodResponse(200)
                .setMessageResponse("Depósito registrado correctamente")
                .build();
    }

    private AccountValidationResponse buildTransferSuccessResponse(AccountValidationRequest request) {
        return AccountValidationResponse.newBuilder()
                .setTransactionId(request.getTransactionId())
                .setAccountNumber(request.getAccountNumber())
                .setCodResponse(200)
                .setMessageResponse("Transferencia registrada correctamente")
                .build();
    }

    private AccountValidationResponse buildInsufficientFundsResponse(AccountValidationRequest request) {
        return AccountValidationResponse.newBuilder()
                .setTransactionId(request.getTransactionId())
                .setAccountNumber(request.getAccountNumber())
                .setCodResponse(400)
                .setMessageResponse("Fondos insuficientes")
                .build();
    }

    private Mono<AccountValidationResponse> buildAccountNotFoundResponse(AccountValidationRequest request) {
        log.error("❌ Cuenta no encontrada: {}", request.getAccountNumber());

        return Mono.just(AccountValidationResponse.newBuilder()
                .setTransactionId(request.getTransactionId())
                .setAccountNumber(request.getAccountNumber())
                .setCodResponse(404)
                .setMessageResponse("Cuenta no encontrada")
                .build());
    }

    private Mono<AccountValidationResponse> buildErrorResponse(
            AccountValidationRequest request,
            Throwable error) {

        log.error("💥 Error procesando transacción: {}", error.getMessage(), error);

        return Mono.just(AccountValidationResponse.newBuilder()
                .setTransactionId(request.getTransactionId())
                .setAccountNumber(request.getAccountNumber())
                .setCodResponse(500)
                .setMessageResponse("Error interno del servidor")
                .build());
    }

    private AccountValidationResponse buildTargetMissingResponse(AccountValidationRequest request) {
        return AccountValidationResponse.newBuilder()
                .setTransactionId(request.getTransactionId())
                .setAccountNumber(request.getAccountNumber())
                .setCodResponse(400)
                .setMessageResponse("Cuenta destino inválida")
                .build();
    }
}
