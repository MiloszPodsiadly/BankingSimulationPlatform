package com.milosz.podsiadly.domain.risk.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Przykład DTO dla zagregowanych metryk ryzyka, np. dla dashboardu
public record RiskMetricDto(
        String metricName,
        BigDecimal value,
        String unit,
        LocalDateTime timestamp,
        String description
) {}