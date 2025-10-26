package com.ettdata.account_service.application.port.out;

import com.ettdata.avro.AccountValidationResponse;
import reactor.core.publisher.Mono;

public interface AccountResponseOutputPort {
    /**
     * Publica una respuesta de validación
     * @param accountNumber número de cuenta (usado como key en Kafka)
     * @param response respuesta a publicar
     * @return confirmación de publicación
     */
    Mono<AccountValidationResponse> publishResponse(String accountNumber, AccountValidationResponse response);
}
