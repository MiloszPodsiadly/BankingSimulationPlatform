package com.milosz.podsiadly.domain.simulation.data.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

// Poprawione na rekord
public record WorldBankData(
        List<PageInfo> pageInfo,
        List<IndicatorData> indicatorData
) {
    // Zagnieżdżony rekord dla PageInfo
    public record PageInfo(
            int page,
            int pages,
            int per_page,
            int total
    ) {}

    // Zagnieżdżony rekord dla IndicatorData
    public record IndicatorData(
            Indicator indicator,
            Country country,
            String countryiso3code,
            String date,
            BigDecimal value, // The actual economic data value
            @JsonProperty("unit") String unit,
            @JsonProperty("obs_status") String obsStatus,
            @JsonProperty("decimal") int decimal
    ) {
        // Zagnieżdżony rekord dla Indicator
        public record Indicator(
                String id,
                String value
        ) {}

        // Zagnieżdżony rekord dla Country
        public record Country(
                String id,
                String value
        ) {}
    }
}