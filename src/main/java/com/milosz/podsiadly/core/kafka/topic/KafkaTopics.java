package com.milosz.podsiadly.core.kafka.topic;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Component that holds Kafka topic names.
 * Automatically bound from spring.kafka.topics.* properties.
 */
@Component
@ConfigurationProperties(prefix = "spring.kafka.topics")
@Getter
@Setter
public class KafkaTopics {

    private String accountCreated;
    private String transactionCompleted;
    private String transactionFailed;
    private String userRegistered;
    private String simulationStep;

    // Możesz dodać kolejne tematy tutaj:
    // private String loanApplicationReceived;
}
