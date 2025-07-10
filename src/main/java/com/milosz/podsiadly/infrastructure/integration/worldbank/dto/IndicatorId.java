package com.milosz.podsiadly.infrastructure.integration.worldbank.dto;

// Rekord dla obiektu wskaźnika w odpowiedzi
public record IndicatorId(
        String id,
        String value
) {}