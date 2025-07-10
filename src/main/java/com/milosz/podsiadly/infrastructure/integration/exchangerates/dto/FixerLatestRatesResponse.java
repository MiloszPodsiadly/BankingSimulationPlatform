package com.milosz.podsiadly.infrastructure.integration.exchangerates.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record FixerLatestRatesResponse(
        boolean success,
        Long timestamp,
        String base,
        String date,
        Map<String, Double> rates,
        FixerError error
) {}