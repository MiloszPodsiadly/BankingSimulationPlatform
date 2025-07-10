package com.milosz.podsiadly.core.kafka.consumer;

import com.milosz.podsiadly.core.event.*;
import com.milosz.podsiadly.core.kafka.topic.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final GeneralEventListener generalEventListener;

    public KafkaConsumerService(GeneralEventListener generalEventListener) {
        this.generalEventListener = generalEventListener;
    }

    @KafkaListener(topics = "${spring.kafka.topics.account-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenAccountCreatedEvent(AccountCreatedEvent event) {
        log.info("KafkaConsumerService: Received AccountCreatedEvent for account ID: {}", event.getAccountId());
        generalEventListener.handleAccountCreatedEvent(event);
    }

    @KafkaListener(topics = "${spring.kafka.topics.transaction-completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenTransactionCompletedEvent(TransactionCompletedEvent event) {
        log.info("KafkaConsumerService: Received TransactionCompletedEvent for transaction ID: {}", event.getTransactionId());
        generalEventListener.handleTransactionCompletedEvent(event);
    }

    @KafkaListener(topics = "${spring.kafka.topics.transaction-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenTransactionFailedEvent(TransactionFailedEvent event) {
        log.warn("KafkaConsumerService: Received TransactionFailedEvent for transaction ID: {}. Reason: {}", event.getTransactionId(), event.getReason());
        generalEventListener.handleTransactionFailedEvent(event);
    }

    @KafkaListener(topics = "${spring.kafka.topics.user-registered}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("KafkaConsumerService: Received UserRegisteredEvent for user ID: {}", event.getUserId());
        generalEventListener.handleUserRegisteredEvent(event);
    }

    // Możesz odkomentować, jeśli potrzebujesz:
    /*
    @KafkaListener(topics = "${spring.kafka.topics.simulation-step}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenSimulationStepEvent(SimulationStepEvent event) {
        log.info("KafkaConsumerService: Received SimulationStepEvent for simulation ID: {}", event.getSimulationId());
        generalEventListener.handleSimulationStepEvent(event);
    }
    */
}
