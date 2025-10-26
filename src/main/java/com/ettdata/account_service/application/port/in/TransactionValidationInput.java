package com.ettdata.account_service.application.port.in;

import com.ettdata.avro.AccountValidationRequest;
import com.ettdata.avro.AccountValidationResponse;
import reactor.core.publisher.Mono;

public interface TransactionValidationInput {
    /**
     * Valida y procesa una solicitud de retiro
     * @param request solicitud de validación desde Kafka
     * @return respuesta de validación
     */
    Mono<AccountValidationResponse> retiro(AccountValidationRequest request);

    /**
     * Valida y procesa una solicitud de depósito
     */
    Mono<AccountValidationResponse> deposito(AccountValidationRequest request);

    /**
     * Valida y procesa una solicitud de transferencia (validación de cuenta origen)
     */
    Mono<AccountValidationResponse> transferencia(AccountValidationRequest request);
}
