package com.ettdata.account_service.infrastructure.kafka;

import com.ettdata.account_service.application.port.in.TransactionValidationInput;
import com.ettdata.account_service.application.port.out.AccountResponseOutputPort;
import com.ettdata.avro.AccountValidationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;


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

        log.info("📨 Mensaje recibido: transactionId={}, account={}, amount={}",
                transactionId, request.getAccountNumber(), request.getAmount());

        validateWithdrawUseCase.retiro(request)
                .flatMap(response ->
                        responsePublisher.publishResponse(
                                String.valueOf(request.getAccountNumber()),
                                response
                        )
                )
                .doOnSuccess(response ->
                        log.info("✅ Procesamiento exitoso: transactionId={}", transactionId))
                .doOnError(error ->
                        log.error("❌ Error procesando: transactionId={}, error={}",
                                transactionId, error.getMessage()))
                .doFinally(signal -> {
                    ack.acknowledge();
                    log.debug("✔️ ACK enviado: transactionId={}", transactionId);
                })
                .subscribe();
    }
}