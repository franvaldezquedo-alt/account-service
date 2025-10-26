package com.ettdata.account_service.infrastructure.config;

import com.ettdata.avro.AccountValidationRequest;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {
    private final KafkaProperties kafkaProperties; // ✅ Inyecta las propiedades de Spring Boot

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AccountValidationRequest> manualAckKafkaListenerContainerFactory(
            ConsumerFactory<String, AccountValidationRequest> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, AccountValidationRequest> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, AccountValidationRequest> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Usa las propiedades del application.yml
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProperties.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                kafkaProperties.getConsumer().getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                KafkaAvroDeserializer.class);

        // Schema Registry desde application.yml
        props.put("schema.registry.url",
                kafkaProperties.getConsumer().getProperties().get("schema.registry.url"));
        props.put("specific.avro.reader",
                kafkaProperties.getConsumer().getProperties().get("specific.avro.reader"));

        return new DefaultKafkaConsumerFactory<>(props);
    }
}
