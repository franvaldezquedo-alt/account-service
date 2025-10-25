package com.ettdata.account_service.infrastructure.kafka;

import com.ettdata.avro.AccountValidationRequest;
import com.ettdata.avro.AccountValidationResponse;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountEventProducer {
    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
    private static final String TOPIC_ACCOUNT_CREATED = "account-created";
    private static final String TOPIC_ACCOUNT_VALIDATION_RESPONSE = "account-validation-response";

    public AccountEventProducer(KafkaTemplate<String, SpecificRecord> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public Mono<Void> sendAccountCreatedEvent(String accountNumber, BigDecimal initialAmount) {
        AccountValidationRequest request = AccountValidationRequest.newBuilder()
                .setTransactionId(UUID.randomUUID().toString())
                .setAccountNumber(accountNumber)
                .setAmount(initialAmount != null ? initialAmount.doubleValue() : null)
                .setTransactionType("ACCOUNT_CREATED")
                .build();

        kafkaTemplate.send(TOPIC_ACCOUNT_CREATED, accountNumber, request);
        return Mono.empty();
    }

    public void sendAccountValidationResponse(AccountValidationResponse response) {
        kafkaTemplate.send(TOPIC_ACCOUNT_VALIDATION_RESPONSE, response.getAccountNumber().toString(), response);
    }
}
