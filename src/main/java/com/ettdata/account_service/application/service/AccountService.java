package com.ettdata.account_service.application.service;

import com.ettdata.account_service.application.port.in.AccountInputPort;
import com.ettdata.account_service.application.port.out.AccountRepositoryOutputPort;
import com.ettdata.account_service.application.port.out.CustomerOutputPort;
import com.ettdata.account_service.domain.error.AccountNotFoundException;
import com.ettdata.account_service.domain.model.AccountListResponse;
import com.ettdata.account_service.domain.model.AccountResponse;
import com.ettdata.account_service.infrastructure.model.AccountRequest;
import com.ettdata.account_service.infrastructure.utils.AccountConstants;
import com.ettdata.account_service.infrastructure.utils.AccountUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Servicio de aplicación para la gestión de cuentas bancarias.
 * Implementa las reglas de negocio definidas para el sistema bancario.
 */
@Service
@Slf4j
public class AccountService implements AccountInputPort {

    private final AccountRepositoryOutputPort accountRepositoryOutputPort;
    private final CustomerOutputPort customerClientPort;

    public AccountService(AccountRepositoryOutputPort accountRepositoryOutputPort,
                          CustomerOutputPort customerClientPort) {
        this.accountRepositoryOutputPort = accountRepositoryOutputPort;
        this.customerClientPort = customerClientPort;
    }

    @Override
    public Mono<AccountListResponse> findAllBankAccount() {
        return accountRepositoryOutputPort.findAllBankAccount()
                .collectList()
                .map(AccountUtils::converBankAccountListResponse)
                .doOnSuccess(res -> log.debug("se encontraron las {} cuentas",
                        res.getData() != null ? res.getData().size() : 0))
                .doOnError(error-> log.error("error al consultar cuentas"));
    }

    /**
     * Crea una nueva cuenta bancaria aplicando validaciones de negocio.
     *
     * @param request datos de la cuenta a crear
     * @return respuesta con código, mensaje y ID de la cuenta creada
     */
    @Override
    public Mono<AccountResponse> createBankAccount(AccountRequest request) {
        log.info("Iniciando creación de cuenta bancaria para documento: {}", request.getCustomerDocument());

        return customerClientPort.getCustomerByDocument(request.getCustomerDocument())
                .flatMap(customer -> {
                    log.info("Cliente encontrado - Tipo: {}, ID: {}", customer.getCustomerType(), customer.getId());

                    // Validar monto inicial mínimo de apertura
                    if (request.getInitialBalance().compareTo(request.getMinimumOpeningAmount()) < 0) {
                        log.warn("Saldo inicial ({}) menor al mínimo requerido ({})",
                                request.getInitialBalance(), request.getMinimumOpeningAmount());
                        return Mono.just(AccountUtils.createErrorResponse(
                                AccountConstants.HTTP_BAD_REQUEST,
                                AccountConstants.ACCOUNT_MIN_BALANCE_ERROR));
                    }

                    // Validar tipo de cuenta permitido según tipo de cliente
                    if (!isAccountTypeAllowed(customer.getCustomerType(), request.getAccountType())) {
                        log.warn("Tipo de cuenta '{}' no permitido para cliente tipo '{}'",
                                request.getAccountType(), customer.getCustomerType());
                        return Mono.just(AccountUtils.createErrorResponse(
                                AccountConstants.HTTP_BAD_REQUEST,
                                AccountConstants.ACCOUNT_TYPE_NOT_ALLOWED));
                    }

                    // Validar límite de cuentas por cliente
                    return accountRepositoryOutputPort.findByCustomerId(customer.getId())
                            .collectList()
                            .flatMap(existingAccounts -> {

                                // Validación: cliente PERSONAL solo puede tener una de cada tipo
                                if ("PERSONAL".equalsIgnoreCase(customer.getCustomerType())) {
                                    boolean hasAccountOfType = existingAccounts.stream()
                                            .anyMatch(acc -> acc.getAccountType().name()
                                                    .equalsIgnoreCase(request.getAccountType()));

                                    if (hasAccountOfType) {
                                        log.warn("Cliente PERSONAL ya tiene una cuenta del tipo '{}'",
                                                request.getAccountType());
                                        return Mono.just(AccountUtils.createErrorResponse(
                                                AccountConstants.HTTP_BAD_REQUEST,
                                                AccountConstants.ACCOUNT_ALREADY_EXISTS));
                                    }
                                }

                                // Validación extra: clientes VIP y PYME (por implementar tarjetas)
                                if ("VIP".equalsIgnoreCase(customer.getCustomerType())
                                        && "SAVINGS".equalsIgnoreCase(request.getAccountType())) {
                                    log.info("Cliente VIP solicitando cuenta de ahorro - validar requisito de tarjeta");
                                }

                                if ("PYME".equalsIgnoreCase(customer.getCustomerType())
                                        && "CURRENT".equalsIgnoreCase(request.getAccountType())) {
                                    log.info("Cliente PYME solicitando cuenta corriente - validar beneficio sin comisión");
                                }

                                // Crear y guardar cuenta
                                return Mono.just(request)
                                        .map(req -> AccountUtils.convertRequestToEntity(req, customer.getId()))
                                        .flatMap(accountRepositoryOutputPort::saveBankAccount)
                                        .map(AccountUtils::convertEntityToResponse)
                                        .doOnSuccess(res -> log.info("Cuenta creada exitosamente - ID: {}, Tipo: {}",
                                                res.getCodEntity(), request.getAccountType()));
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Cliente no encontrado con documento: {}", request.getCustomerDocument());
                    return Mono.just(AccountUtils.createErrorResponse(
                            AccountConstants.HTTP_BAD_REQUEST,
                            AccountConstants.CUSTOMER_NOT_FOUND));
                }))
                .onErrorResume(ex -> {
                    log.error("Error inesperado en creación de cuenta: {}", ex.getMessage(), ex);
                    return Mono.just(AccountUtils.createErrorResponse(
                            AccountConstants.HTTP_INTERNAL_ERROR,
                            "Error al procesar la solicitud: " + ex.getMessage()));
                });
    }

    @Override
    public Mono<AccountListResponse> findByIdBankAccount(String id) {
        return accountRepositoryOutputPort.findByIdBankAccount(id)
                .map(AccountUtils::ConvertBackAccountSingletonResponse)
                .doOnSuccess(res -> log.debug("Cuenta encontrada con id: {}", id))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Cuenta no encontrada con id: {}", id);
                    return Mono.error(new AccountNotFoundException(AccountConstants.BANK_ACCOUNT_NOT_FOUND + id));
                }))
                .doOnError(error -> log.error("Error al consultar cuenta con id {}: {}", id, error.getMessage()));
    }

    @Override
    public Mono<AccountResponse> deleteByIdBankAccount(String id) {
        return accountRepositoryOutputPort.deleteByIdBankAccount(id)
                .then(Mono.fromCallable(() -> AccountUtils.convertBankAccountResponseDelete(id)))
                .doOnSuccess(res -> log.info("Cuenta eliminada exitosamente con id: {}", id));
    }

    /**
     * Valida si un tipo de cliente puede abrir un tipo específico de cuenta.
     */
    private boolean isAccountTypeAllowed(String customerType, String accountType) {
        if (customerType == null || accountType == null) {
            log.warn("Parámetros nulos en validación de tipo de cuenta");
            return false;
        }

        String ct = customerType.toUpperCase();
        String at = accountType.toUpperCase();

        switch (ct) {
            case "PERSONAL":
                return at.equals("SAVINGS") || at.equals("CURRENT") || at.equals("FIXED_TERM");
            case "BUSINESS":
            case "PYME":
                return at.equals("CURRENT");
            case "VIP":
                return at.equals("SAVINGS") || at.equals("CURRENT");
            default:
                log.warn("Tipo de cliente desconocido: {}", customerType);
                return false;
        }
    }
}
