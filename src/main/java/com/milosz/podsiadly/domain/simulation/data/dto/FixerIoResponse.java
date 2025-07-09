package com.milosz.podsiadly.domain.simulation.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
public record FixerIoResponse(
        boolean success,
        LocalDate date,
        long timestamp,
        String base,
        @JsonProperty("rates") Map<String, BigDecimal> rates, // Key: currency code, Value: exchange rate
        String error // For error responses
) {}