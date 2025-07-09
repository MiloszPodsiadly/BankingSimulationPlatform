package com.milosz.podsiadly.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AppConfig {

    //@Bean
    //@Primary // Ensures this ObjectMapper is preferred if multiple are present
    //public ObjectMapper objectMapper() {
       // ObjectMapper mapper = new ObjectMapper();
        //mapper.registerModule(new JavaTimeModule()); // Support for Java 8 Date & Time API
        //mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Serialize dates as ISO 8601 strings
       // return mapper;
    //}
}