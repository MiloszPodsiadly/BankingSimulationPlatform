package com.milosz.podsiadly.core.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for sending messages to Apache Kafka topics.
 * It encapsulates the KafkaTemplate and provides methods for asynchronous sending
 * with logging and basic error handling.
 */
@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Constructor for KafkaProducerService.
     * Spring will automatically inject the configured KafkaTemplate.
     *
     * @param kafkaTemplate The KafkaTemplate instance used for sending messages.
     */
    public KafkaProducerService(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a message to a specified Kafka topic.
     * The message is sent asynchronously, and the result is handled by a CompletableFuture.
     *
     * @param topic   The Kafka topic to which the message will be sent.
     * @param message The message object to be sent. This object will be serialized
     * by the configured Kafka serializer (e.g., JsonSerializer).
     */
    public void sendMessage(String topic, Object message) {
        log.info("Attempting to send message to topic: '{}', message: {}", topic, message);

        // Send the message asynchronously and get a CompletableFuture
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, message);

        // Handle the result of the asynchronous send operation
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // Message sent successfully
                log.info("Message sent successfully to topic: '{}', offset: {}, partition: {}",
                        topic, result.getRecordMetadata().offset(), result.getRecordMetadata().partition());
            } else {
                // Failed to send message
                log.error("Failed to send message to topic: '{}', message: {}. Error: {}",
                        topic, message, ex.getMessage(), ex);
                // Here you might want to implement more sophisticated error handling,
                // e.g., retry mechanisms, dead-letter queue (DLQ) processing.
            }
        });
    }
}
