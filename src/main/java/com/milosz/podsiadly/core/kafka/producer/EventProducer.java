package com.milosz.podsiadly.core.kafka.producer;

import com.milosz.podsiadly.core.event.AccountCreatedEvent;
import com.milosz.podsiadly.core.event.TransactionCompletedEvent; // Importuj nowe zdarzenie
import com.milosz.podsiadly.core.event.TransactionFailedEvent; // Importuj nowe zdarzenie
import com.milosz.podsiadly.core.event.UserRegisteredEvent; // Pozostaw, jeśli używasz
import com.milosz.podsiadly.core.event.SimulationStepEvent; // Pozostaw, jeśli używasz
import com.milosz.podsiadly.core.kafka.topic.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Producer responsible for sending specific domain events to Apache Kafka.
 * It uses the generic KafkaProducerService to handle the actual sending mechanism
 * and maps domain events to their respective Kafka topics.
 */
@Component
public class EventProducer {

    private static final Logger log = LoggerFactory.getLogger(EventProducer.class);

    private final KafkaProducerService kafkaProducerService;
    private final KafkaTopics kafkaTopics;

    /**
     * Constructor for EventProducer.
     * Spring will automatically inject KafkaProducerService and KafkaTopics.
     *
     * @param kafkaProducerService The service used for sending messages to Kafka.
     * @param kafkaTopics The utility class providing Kafka topic names.
     */
    public EventProducer(KafkaProducerService kafkaProducerService, KafkaTopics kafkaTopics) {
        this.kafkaProducerService = kafkaProducerService;
        this.kafkaTopics = kafkaTopics;
    }

    /**
     * Sends an AccountCreatedEvent to the appropriate Kafka topic.
     *
     * @param event The AccountCreatedEvent to be sent.
     */
    public void publishAccountCreatedEvent(AccountCreatedEvent event) {
        log.info("Publishing AccountCreatedEvent for account ID: {}", event.getAccountId());
        kafkaProducerService.sendMessage(kafkaTopics.getAccountCreated(), event);
    }

    /**
     * Sends a TransactionCompletedEvent to the appropriate Kafka topic.
     *
     * @param event The TransactionCompletedEvent to be sent.
     */
    public void publishTransactionCompletedEvent(TransactionCompletedEvent event) {
        log.info("Publishing TransactionCompletedEvent for transaction ID: {}", event.getTransactionId());
        kafkaProducerService.sendMessage(kafkaTopics.getTransactionCompleted(), event);
    }

    /**
     * Sends a TransactionFailedEvent to the appropriate Kafka topic.
     *
     * @param event The TransactionFailedEvent to be sent.
     */
    public void publishTransactionFailedEvent(TransactionFailedEvent event) {
        log.warn("Publishing TransactionFailedEvent for transaction ID: {}. Reason: {}", event.getTransactionId(), event.getReason());
        kafkaProducerService.sendMessage(kafkaTopics.getTransactionFailed(), event);
    }

    /**
     * Sends a UserRegisteredEvent to the appropriate Kafka topic.
     *
     * @param event The UserRegisteredEvent to be sent.
     */
    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Publishing UserRegisteredEvent for user ID: {}", event.getUserId());
        kafkaProducerService.sendMessage(kafkaTopics.getUserRegistered(), event);
    }

    /**
     * Sends a SimulationStepEvent to the appropriate Kafka topic.
     *
     * @param event The SimulationStepEvent to be sent.
     */
    public void publishSimulationStepEvent(SimulationStepEvent event) {
        log.info("Publishing SimulationStepEvent for simulation ID: {}", event.getSimulationId());
        kafkaProducerService.sendMessage(kafkaTopics.getSimulationStep(), event);
    }
}
