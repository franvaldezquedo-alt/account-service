package com.ettdata.account_service.infrastructure.kafka;

import com.ettdata.account_service.application.port.in.TransactionValidationInput;
import com.ettdata.account_service.application.port.out.AccountResponseOutputPort;
import com.ettdata.avro.AccountValidationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Slf4j
@Service
@RequiredArgsConstructor
public class AccountEventConsumer {
    private final TransactionValidationInput validateWithdrawUseCase;
    private final AccountResponseOutputPort responsePublisher;

    @KafkaListener(
            topics = "${kafka.topics.account-validation-request}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "manualAckKafkaListenerContainerFactory"
    )
    public void consumeWithdrawEvent(AccountValidationRequest request, Acknowledgment ack) {
        String transactionId = String.valueOf(request.getTransactionId());

        log.info("📨 Mensaje recibido: transactionId={}, account={}, amount={}, type={}",
                transactionId, request.getAccountNumber(), request.getAmount(), request.getTransactionType());

        // Seleccionar el flujo de validación según el tipo de transacción
        Mono<com.ettdata.avro.AccountValidationResponse> validationFlow;

        String transactionType = request.getTransactionType() == null ? "" : request.getTransactionType().toUpperCase();

        switch (transactionType) {
            case "WITHDRAWAL":
                validationFlow = validateWithdrawUseCase.retiro(request);
                break;
            case "DEPOSIT":
                validationFlow = validateWithdrawUseCase.deposito(request);
                break;
            case "TRANSFER":
                validationFlow = validateWithdrawUseCase.transferencia(request);
                break;
            default:
                log.warn("❗ Tipo de transacción desconocido: {}", request.getTransactionType());
                validationFlow = Mono.just(com.ettdata.avro.AccountValidationResponse.newBuilder()
                        .setTransactionId(request.getTransactionId())
                        .setAccountNumber(request.getAccountNumber())
                        .setCodResponse(400)
                        .setMessageResponse("Tipo de transacción inválido")
                        .build());
        }

        validationFlow
                .flatMap(response -> responsePublisher.publishResponse(String.valueOf(request.getAccountNumber()), response))
                .doOnSuccess(resp -> log.info("✅ Procesamiento exitoso: transactionId={}", transactionId))
                .doOnError(error -> log.error("❌ Error procesando: transactionId={}, error={}", transactionId, error.getMessage()))
                .doFinally(signal -> {
                    ack.acknowledge();
                    log.debug("✔️ ACK enviado: transactionId={}", transactionId);
                })
                .subscribe();
    }
}