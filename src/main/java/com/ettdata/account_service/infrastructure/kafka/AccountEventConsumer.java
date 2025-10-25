package com.ettdata.account_service.infrastructure.kafka;

import com.ettdata.avro.AccountValidationRequest;
import com.ettdata.avro.AccountValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountEventConsumer {

   private final AccountEventProducer accountEventProducer;

    @KafkaListener(topics = "account-validation-request", groupId = "account-service-group")
    public void consumeValidationRequest(AccountValidationRequest request) {
        log.info("📩 Recibido evento de validación de cuenta: {}", request);

        // Simulación de validación: en un caso real consultarías la base de datos
        boolean isValid = request.getAmount() <= 1000.0; // ejemplo simple

        // Construcción del mensaje de respuesta
        AccountValidationResponse response = AccountValidationResponse.newBuilder()
                .setTransactionId(request.getTransactionId())
                .setAccountNumber(request.getAccountNumber())
                // Usa setValid(...) si tu schema ya se actualizó
                .setIsValid(isValid)
                .setReason(isValid ? "Cuenta válida" : "Fondos insuficientes")
                .build();

        log.info("📤 Enviando respuesta de validación: {}", response);

        // Envío del evento de respuesta al topic de validación
        accountEventProducer.sendAccountValidationResponse(response);
    }
}
