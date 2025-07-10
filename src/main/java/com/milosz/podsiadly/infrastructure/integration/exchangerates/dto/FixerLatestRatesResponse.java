package com.milosz.podsiadly.infrastructure.integration.exchangerates.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import com.milosz.podsiadly.infrastructure.integration.exchangerates.dto.ErrorDetails; // Ten import jest kluczowy!

public record FixerLatestRatesResponse(
        boolean success,
        Long timestamp,
        String base,
        String date,
        Map<String, Double> rates,
        ErrorDetails error
) {}