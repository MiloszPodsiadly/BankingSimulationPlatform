package com.milosz.podsiadly.core.kafka.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // Ważne dla serializacji LocalDateTime
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Custom Kafka Serializer for converting Java objects (events) to JSON format.
 * It uses Jackson's ObjectMapper for serialization, supporting Java 8 Date and Time API.
 */
public class JsonEventSerializer<T> implements Serializer<T> {

    private static final Logger log = LoggerFactory.getLogger(JsonEventSerializer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonEventSerializer() {
        // Register JavaTimeModule to correctly serialize and deserialize Java 8 Date and Time objects (e.g., LocalDateTime)
        objectMapper.registerModule(new JavaTimeModule());
        // Configure ObjectMapper to NOT fail on unknown properties during deserialization
        // and to NOT write null values, making JSON output cleaner.
        // objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // For deserializer, but good to know
        // objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // Optional: do not include null fields
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Nothing to configure for now
        // If there were specific configurations for the serializer, they would be handled here.
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            log.warn("Null data received for serialization on topic: {}", topic);
            return null;
        }
        try {
            // Convert Java object to JSON byte array
            byte[] bytes = objectMapper.writeValueAsBytes(data);
            log.debug("Serialized data for topic {}: {}", topic, new String(bytes));
            return bytes;
        } catch (Exception e) {
            log.error("Error serializing JSON for topic {}: {}", topic, e.getMessage(), e);
            throw new SerializationException("Error serializing JSON message for topic " + topic, e);
        }
    }

    @Override
    public void close() {
        // Nothing to close for now
        // Resources specific to the serializer would be closed here if necessary.
    }
}
