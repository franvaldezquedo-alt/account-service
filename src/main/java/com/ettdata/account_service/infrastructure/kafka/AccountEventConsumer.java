package com.ettdata.account_service.infrastructure.kafka;

import com.ettdata.account_service.application.port.out.AccountRepositoryOutputPort;
import com.ettdata.avro.AccountValidationRequest;
import com.ettdata.avro.AccountValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountEventConsumer {

    private final AccountRepositoryOutputPort accountRepository;
    private final KafkaTemplate<String, AccountValidationResponse> kafkaTemplate;

    private static final String TOPIC_RESPONSE = "account-validation-response";

    @KafkaListener(
            topics = "account-validation-request",
            groupId = "account-service-group",
            containerFactory = "manualAckKafkaListenerContainerFactory"
    )
    public void consumeWithdrawEvent(AccountValidationRequest request, Acknowledgment ack) {
        log.info("📨 Mensaje recibido: transactionId={}, accountNumber={}, amount={}",
                request.getTransactionId(), request.getAccountNumber(), request.getAmount());

        accountRepository.findByNumberAccount(request.getAccountNumber().toString())
                .flatMap(account -> {
                    log.info("🔍 Cuenta encontrada: {}, saldo actual: {}",
                            account.getAccountNumber(), account.getBalance());

                    AccountValidationResponse.Builder responseBuilder = AccountValidationResponse.newBuilder()
                            .setTransactionId(request.getTransactionId())
                            .setAccountNumber(request.getAccountNumber());

                    BigDecimal requestAmount = BigDecimal.valueOf(request.getAmount());

                    if (account.getBalance().compareTo(requestAmount) < 0) {
                        log.warn("⚠️ Fondos insuficientes: cuenta={}, saldo={}, monto solicitado={}",
                                account.getAccountNumber(), account.getBalance(), requestAmount);

                        AccountValidationResponse errorResponse = responseBuilder
                                .setCodResponse(400)
                                .setMessageResponse("Fondos insuficientes")
                                .build();

                        return sendResponse(request.getAccountNumber().toString(), errorResponse);
                    }

                    // Actualizar saldo
                    account.setBalance(account.getBalance().subtract(requestAmount));

                    return accountRepository.saveOrUpdateAccount(account)
                            .flatMap(updated -> {
                                log.info("✅ Retiro aplicado correctamente: cuenta={}, nuevo saldo={}",
                                        updated.getAccountNumber(), updated.getBalance());

                                AccountValidationResponse successResponse = responseBuilder
                                        .setCodResponse(200)
                                        .setMessageResponse("Retiro registrado correctamente")
                                        .build();

                                return sendResponse(updated.getAccountNumber(), successResponse);
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("❌ Cuenta no encontrada: {}", request.getAccountNumber());

                    AccountValidationResponse notFoundResponse = AccountValidationResponse.newBuilder()
                            .setTransactionId(request.getTransactionId())
                            .setAccountNumber(request.getAccountNumber())
                            .setCodResponse(404)
                            .setMessageResponse("Cuenta no encontrada")
                            .build();

                    return sendResponse(request.getAccountNumber().toString(), notFoundResponse);
                }))
                .doOnError(error -> {
                    log.error("💥 Error procesando retiro: {}", error.getMessage(), error);

                    AccountValidationResponse errorResponse = AccountValidationResponse.newBuilder()
                            .setTransactionId(request.getTransactionId())
                            .setAccountNumber(request.getAccountNumber())
                            .setCodResponse(500)
                            .setMessageResponse("Error interno: " + error.getMessage())
                            .build();

                    sendResponse(request.getAccountNumber().toString(), errorResponse)
                            .subscribe();
                })
                .doFinally(signal -> {
                    log.info("✔️ Mensaje procesado, enviando ACK para transactionId={}", request.getTransactionId());
                    ack.acknowledge();
                })
                .subscribe();
    }

    /**
     * Envía la respuesta a Kafka y retorna un Mono que se completa cuando el envío es exitoso
     * ✅ JAVA 11 - ListenableFuture con addCallback
     */
    private Mono<Void> sendResponse(String accountNumber, AccountValidationResponse response) {
        return Mono.create(sink -> {
            kafkaTemplate.send(TOPIC_RESPONSE, accountNumber, response)
                    .addCallback(
                            result -> {
                                log.info("📤 Respuesta enviada a Kafka: transactionId={}, codResponse={}, mensaje={}",
                                        response.getTransactionId(), response.getCodResponse(), response.getMessageResponse());
                                sink.success();
                            },
                            ex -> {
                                log.error("❌ Error enviando respuesta a Kafka: {}", ex.getMessage(), ex);
                                sink.error(ex);
                            }
                    );
        });
    }
}