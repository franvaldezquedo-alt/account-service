package com.ettdata.account_service.infrastructure.kafka;

import com.ettdata.account_service.application.port.out.AccountResponseOutputPort;
import com.ettdata.account_service.application.service.AccountValidationService;
import com.ettdata.account_service.infrastructure.config.KafkaTopicProperties;
import com.ettdata.avro.AccountValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAccountResponsePublisher implements AccountResponseOutputPort {
    private final KafkaTemplate<String, AccountValidationResponse> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    @Override
    public Mono<AccountValidationResponse> publishResponse(String accountNumber, AccountValidationResponse response) {
        log.debug("📤 Publicando respuesta: account={}, transactionId={}",
                accountNumber, response.getTransactionId());

        return Mono.create(sink ->
                kafkaTemplate.send(topicProperties.getAccountValidationResponse(), accountNumber, response)
                        .addCallback(
                                result -> {
                                    log.info("✅ Respuesta publicada: transactionId={}, status={}",
                                            response.getTransactionId(),
                                            response.getCodResponse());
                                    sink.success(response);
                                },
                                ex -> {
                                    log.error("❌ Error publicando respuesta: transactionId={}, error={}",
                                            response.getTransactionId(), ex.getMessage(), ex);
                                    sink.error(ex);
                                }
                        )
        );
    }
}
