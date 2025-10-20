package com.ettdata.account_service.application.service;

import com.ettdata.account_service.application.port.in.BankAccountInputPort;
import com.ettdata.account_service.application.port.out.BankAccountRepositoryOutputPort;
import com.ettdata.account_service.application.port.out.CustomerOutputPort;
import com.ettdata.account_service.domain.error.BankAccountNotFoundException;
import com.ettdata.account_service.domain.model.BankAccountListResponse;
import com.ettdata.account_service.domain.model.BankAccountResponse;
import com.ettdata.account_service.infrastructure.model.BankAccountRequest;
import com.ettdata.account_service.infrastructure.utils.BankAccountConstants;
import com.ettdata.account_service.infrastructure.utils.BankAccountUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Servicio de aplicación para la gestión de cuentas bancarias.
 * Implementa las reglas de negocio definidas para el sistema bancario.
 */
@Service
@Slf4j
public class BankAccountService implements BankAccountInputPort {

    private final BankAccountRepositoryOutputPort bankAccountRepositoryOutputPort;
    private final CustomerOutputPort customerClientPort;

    public BankAccountService(BankAccountRepositoryOutputPort bankAccountRepositoryOutputPort,
                              CustomerOutputPort customerClientPort) {
        this.bankAccountRepositoryOutputPort = bankAccountRepositoryOutputPort;
        this.customerClientPort = customerClientPort;
    }

    @Override
    public Mono<BankAccountListResponse> findAllBankAccount() {
        return bankAccountRepositoryOutputPort.findAllBankAccount()
                .collectList()
                .map(BankAccountUtils::converBankAccountListResponse)
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
    public Mono<BankAccountResponse> createBankAccount(BankAccountRequest request) {
        log.info("Iniciando creación de cuenta bancaria para documento: {}", request.getCustomerDocument());

        return customerClientPort.getCustomerByDocument(request.getCustomerDocument())
                .flatMap(customer -> {
                    log.info("Cliente encontrado - Tipo: {}, ID: {}", customer.getCustomerType(), customer.getId());

                    // Validar monto inicial mínimo de apertura
                    if (request.getInitialBalance().compareTo(request.getMinimumOpeningAmount()) < 0) {
                        log.warn("Saldo inicial ({}) menor al mínimo requerido ({})",
                                request.getInitialBalance(), request.getMinimumOpeningAmount());
                        return Mono.just(BankAccountUtils.createErrorResponse(
                                BankAccountConstants.HTTP_BAD_REQUEST,
                                BankAccountConstants.ACCOUNT_MIN_BALANCE_ERROR));
                    }

                    // Validar tipo de cuenta permitido según tipo de cliente
                    if (!isAccountTypeAllowed(customer.getCustomerType(), request.getAccountType())) {
                        log.warn("Tipo de cuenta '{}' no permitido para cliente tipo '{}'",
                                request.getAccountType(), customer.getCustomerType());
                        return Mono.just(BankAccountUtils.createErrorResponse(
                                BankAccountConstants.HTTP_BAD_REQUEST,
                                BankAccountConstants.ACCOUNT_TYPE_NOT_ALLOWED));
                    }

                    // Validar límite de cuentas por cliente
                    return bankAccountRepositoryOutputPort.findByCustomerId(customer.getId())
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
                                        return Mono.just(BankAccountUtils.createErrorResponse(
                                                BankAccountConstants.HTTP_BAD_REQUEST,
                                                BankAccountConstants.ACCOUNT_ALREADY_EXISTS));
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
                                        .map(req -> BankAccountUtils.convertRequestToEntity(req, customer.getId()))
                                        .flatMap(bankAccountRepositoryOutputPort::saveBankAccount)
                                        .map(BankAccountUtils::convertEntityToResponse)
                                        .doOnSuccess(res -> log.info("Cuenta creada exitosamente - ID: {}, Tipo: {}",
                                                res.getCodEntity(), request.getAccountType()));
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Cliente no encontrado con documento: {}", request.getCustomerDocument());
                    return Mono.just(BankAccountUtils.createErrorResponse(
                            BankAccountConstants.HTTP_BAD_REQUEST,
                            BankAccountConstants.CUSTOMER_NOT_FOUND));
                }))
                .onErrorResume(ex -> {
                    log.error("Error inesperado en creación de cuenta: {}", ex.getMessage(), ex);
                    return Mono.just(BankAccountUtils.createErrorResponse(
                            BankAccountConstants.HTTP_INTERNAL_ERROR,
                            "Error al procesar la solicitud: " + ex.getMessage()));
                });
    }

    @Override
    public Mono<BankAccountListResponse> findByIdBankAccount(String id) {
        return bankAccountRepositoryOutputPort.findByIdBankAccount(id)
                .map(BankAccountUtils::ConvertBackAccountSingletonResponse)
                .doOnSuccess(res -> log.debug("Cuenta encontrada con id: {}", id))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Cuenta no encontrada con id: {}", id);
                    return Mono.error(new BankAccountNotFoundException(BankAccountConstants.BANK_ACCOUNT_NOT_FOUND + id));
                }))
                .doOnError(error -> log.error("Error al consultar cuenta con id {}: {}", id, error.getMessage()));
    }

    @Override
    public Mono<BankAccountResponse> deleteByIdBankAccount(String id) {
        return bankAccountRepositoryOutputPort.deleteByIdBankAccount(id)
                .then(Mono.fromCallable(() -> BankAccountUtils.convertBankAccountResponseDelete(id)))
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
