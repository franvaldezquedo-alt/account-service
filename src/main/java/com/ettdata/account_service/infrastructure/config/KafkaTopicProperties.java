package com.ettdata.account_service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicProperties {
    private String accountValidationRequest;
    private String accountValidationResponse;
}
