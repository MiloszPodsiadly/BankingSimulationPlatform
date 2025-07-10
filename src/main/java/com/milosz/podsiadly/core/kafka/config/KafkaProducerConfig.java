package com.milosz.podsiadly.core.kafka.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Kafka Producer.
 * Sets up the ProducerFactory and KafkaTemplate beans,
 * defining how messages are sent to Kafka.
 */
@Configuration // Oznacza klasę jako klasę konfiguracyjną Springa
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers; // Adresy brokerów Kafka z konfiguracji

    /**
     * Configures the ProducerFactory.
     * This factory is responsible for creating Kafka Producer instances.
     *
     * @return A ProducerFactory configured with necessary properties.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // Ustawienie adresów brokerów
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // Serializator kluczy
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class); // Serializator wartości (JSON)
        // Możesz dodać więcej właściwości producenta, np. ACK, retries, batch size, linger.ms
        // configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Potwierdzenie otrzymania przez wszystkich brokerów
        // configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // Liczba ponownych prób wysyłki
        // configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // Rozmiar partii w bajtach
        // configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1); // Opóźnienie przed wysłaniem partii

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Configures the KafkaTemplate.
     * KafkaTemplate is a high-level abstraction for sending messages to Kafka,
     * built on top of ProducerFactory.
     *
     * @return A KafkaTemplate instance.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
