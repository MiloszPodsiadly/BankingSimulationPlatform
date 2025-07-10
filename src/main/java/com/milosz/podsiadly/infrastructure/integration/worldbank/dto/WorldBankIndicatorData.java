package com.milosz.podsiadly.infrastructure.integration.worldbank.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // Do mapowania JSON na pola Javy

// Rekord dla pojedynczego punktu danych wskaźnika
public record WorldBankIndicatorData(
        IndicatorId indicator,
        CountryId country,
        @JsonProperty("countryiso3code") String countryIso3Code,
        String date,
        Double value,
        String unit,
        @JsonProperty("obs_status") String obsStatus,
        Integer decimal
) {}