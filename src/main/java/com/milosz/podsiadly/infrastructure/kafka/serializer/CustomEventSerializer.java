package com.milosz.podsiadly.infrastructure.kafka.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j // Do logowania
public class CustomEventSerializer<T> implements Serializer<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CustomEventSerializer() {
        // Konfiguracja ObjectMappera, aby prawidłowo serializować obiekty Java Time (np. OffsetDateTime)
        objectMapper.registerModule(new JavaTimeModule());
        // Wyłączenie zapisu dat jako timestampów (domyślnie są to długie liczby)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Opcjonalnie: włączenie pretty printingu dla czytelności JSON-a (do debugowania, nie w produkcji)
        // objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Metoda konfiguracyjna, jeśli potrzebne są jakieś parametry z konfiguracji Kafki.
        // Obecnie nie używamy, ale jest wymagana przez interfejs Serializer.
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            log.warn("Null data received for serialization on topic: {}", topic);
            return null;
        }
        try {
            // Serializuj obiekt T na tablicę bajtów (JSON w naszym przypadku)
            byte[] serializedData = objectMapper.writeValueAsBytes(data);
            log.debug("Serialized data for topic {}: {}", topic, new String(serializedData, StandardCharsets.UTF_8));
            return serializedData;
        } catch (Exception e) {
            log.error("Can't serialize data for topic {}: {}", topic, data, e);
            throw new SerializationException("Error serializing JSON message for topic " + topic, e);
        }
    }

    @Override
    public void close() {
        // Metoda zamykająca zasoby. Obecnie nie ma nic do zamknięcia dla ObjectMappera.
    }
}