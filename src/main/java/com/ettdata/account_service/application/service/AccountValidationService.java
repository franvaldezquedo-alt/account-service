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

    private boolean hasInsufficientFunds(Account account, BigDecimal amount) {
        return account.getBalance().compareTo(amount) < 0;
    }

    private Mono<AccountValidationResponse> processWithdraw(
            AccountValidationRequest request,
            Account account,
            BigDecimal amount) {

        account.setBalance(account.getBalance().subtract(amount));

        return accountRepository.saveOrUpdateAccount(account)
                .map(updated -> {
                    log.info("✅ Retiro aplicado correctamente: cuenta={}, nuevo saldo={}",
                            updated.getAccountNumber(), updated.getBalance());
                    return buildSuccessResponse(request);
                });
    }

    // ========== Response Builders ==========

    private AccountValidationResponse buildSuccessResponse(AccountValidationRequest request) {
        return AccountValidationResponse.newBuilder()
                .setTransactionId(request.getTransactionId())
                .setAccountNumber(request.getAccountNumber())
                .setCodResponse(200)
                .setMessageResponse("Retiro registrado correctamente")
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

        log.error("💥 Error procesando retiro: {}", error.getMessage(), error);

        return Mono.just(AccountValidationResponse.newBuilder()
                .setTransactionId(request.getTransactionId())
                .setAccountNumber(request.getAccountNumber())
                .setCodResponse(500)
                .setMessageResponse("Error interno del servidor")
                .build());
    }
}
