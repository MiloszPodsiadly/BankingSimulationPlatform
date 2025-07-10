package com.milosz.podsiadly.core.kafka.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for Kafka Consumer.
 * Sets up the ConsumerFactory and ConcurrentKafkaListenerContainerFactory beans,
 * defining how messages are received from Kafka.
 */
@EnableKafka // Włącza wsparcie dla adnotacji @KafkaListener
@Configuration // Oznacza klasę jako klasę konfiguracyjną Springa
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers; // Adresy brokerów Kafka z konfiguracji

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId; // ID grupy konsumentów z konfiguracji

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset; // Strategia resetowania offsetu

    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
    private String trustedPackages; // Zaufane pakiety dla deserializacji JSON

    /**
     * Configures the ConsumerFactory.
     * This factory is responsible for creating Kafka Consumer instances.
     *
     * @return A ConsumerFactory configured with necessary properties.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); // Ustawienie adresów brokerów
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId); // Ustawienie ID grupy konsumentów
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class); // Deserializator kluczy
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class); // Deserializator wartości (JSON)
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset); // Strategia resetowania offsetu
        props.put(JsonDeserializer.TRUSTED_PACKAGES, trustedPackages); // Ustawienie zaufanych pakietów dla JsonDeserializer

        // Możesz dodać więcej właściwości konsumenta, np. max.poll.records, enable.auto.commit
        // props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500); // Maksymalna liczba rekordów w jednej partii
        // props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Wyłączenie auto-commita dla ręcznego zarządzania offsetami

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Configures the ConcurrentKafkaListenerContainerFactory.
     * This factory is used to create the listener containers for @KafkaListener annotated methods.
     * It uses the ConsumerFactory to create consumer instances.
     *
     * @return A ConcurrentKafkaListenerContainerFactory instance.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Możesz dodać więcej konfiguracji dla fabryki kontenerów, np. concurrency, filter
        // factory.setConcurrency(3); // Liczba wątków konsumenta
        // factory.setRecordFilterStrategy(record -> record.value().toString().contains("filter_criteria")); // Strategia filtrowania
        return factory;
    }
}
