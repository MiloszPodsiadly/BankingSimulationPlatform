package com.milosz.podsiadly.infrastructure.integration.worldbank.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// Rekord dla metadanych odpowiedzi (pierwszy element listy)
public record WorldBankMetadata(
        Integer page,
        Integer pages,
        @JsonProperty("per_page") String perPage, // Może być stringiem w API
        Integer total,
        @JsonProperty("sourceid") String sourceId,
        @JsonProperty("sourcename") String sourceName,
        @JsonProperty("lastupdated") String lastUpdated // Data aktualizacji, jako string
) {}