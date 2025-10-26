package com.ettdata.account_service.infrastructure.kafka;

import com.ettdata.avro.AccountValidationRequest;
import com.ettdata.avro.AccountValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFutureCallback;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class AccountEventProducer {
  private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
  private static final String TOPIC_ACCOUNT_CREATED = "account-created";
  private static final String TOPIC_ACCOUNT_VALIDATION_RESPONSE = "account-validation-response";

  public AccountEventProducer(KafkaTemplate<String, SpecificRecord> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public Mono<Void> sendAccountCreatedEvent(String accountNumber, BigDecimal initialAmount, String customerId) {
    log.info("📝 Preparando evento Kafka - Account: {}, Amount: {}, Customer: {}",
          accountNumber, initialAmount, customerId);

    return Mono.<Void>create(sink -> {
            try {
              // Validaciones
              if (accountNumber == null || accountNumber.isEmpty()) {
                sink.error(new IllegalArgumentException("accountNumber no puede ser null"));
                return;
              }
              if (customerId == null || customerId.isEmpty()) {
                sink.error(new IllegalArgumentException("customerId no puede ser null"));
                return;
              }

              // Construir el request
              AccountValidationRequest request = AccountValidationRequest.newBuilder()
                      .setTransactionId(UUID.randomUUID().toString())
                      .setAccountNumber(accountNumber)
                      .setTransactionType("WITHDRAWAL")
                      .setAmount(initialAmount != null ? initialAmount.doubleValue() : null)
                      .build();

              log.info("📦 Request Avro construido: transactionId={}, account={}",
                    request.getTransactionId(), request.getAccountNumber());

              // ✅ Usar addCallback para ListenableFuture
              var listenableFuture = kafkaTemplate.send(TOPIC_ACCOUNT_CREATED, accountNumber, request);

              listenableFuture.addCallback(new ListenableFutureCallback<SendResult<String, SpecificRecord>>() {
                @Override
                public void onSuccess(SendResult<String, SpecificRecord> result) {
                  log.info("✅ Evento enviado exitosamente - Topic: {}, Partition: {}, Offset: {}",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                  sink.success();
                }

                @Override
                public void onFailure(Throwable ex) {
                  log.error("❌ Error al enviar evento a Kafka", ex);
                  log.error("❌ Detalles - Account: {}, Customer: {}", accountNumber, customerId);
                  if (ex.getCause() != null) {
                    log.error("❌ Causa raíz: {}", ex.getCause().getMessage());
                  }
                  sink.error(ex);
                }
              });

            } catch (Exception e) {
              log.error("❌ Excepción al construir mensaje Avro", e);
              sink.error(e);
            }
          })
          .subscribeOn(Schedulers.boundedElastic())
          .onErrorResume(error -> {
            log.error("❌ Error final en sendAccountCreatedEvent: {}", error.getMessage(), error);
            return Mono.empty(); // No fallar el flujo principal
          });
  }

  public void sendAccountValidationResponse(AccountValidationResponse response) {
    try {
      log.info("📤 Enviando respuesta de validación: {}", response);

      var listenableFuture = kafkaTemplate.send(
            TOPIC_ACCOUNT_VALIDATION_RESPONSE,
            response.getAccountNumber().toString(),
            response
      );

      // ✅ Usar addCallback
      listenableFuture.addCallback(
            result -> log.info("✅ Respuesta enviada: {}", result.getRecordMetadata()),
            ex -> log.error("❌ Error enviando respuesta", ex)
      );

    } catch (Exception e) {
      log.error("❌ Error al enviar respuesta de validación", e);
    }
  }
}