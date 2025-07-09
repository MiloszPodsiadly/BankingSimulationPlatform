package com.milosz.podsiadly.domain.risk.dto;

import com.milosz.podsiadly.domain.risk.model.RiskIndicator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record RiskIndicatorDto(
        Long id,
        @NotBlank(message = "Indicator code cannot be blank")
        String indicatorCode,
        @NotBlank(message = "Indicator name cannot be blank")
        String name,
        String description,
        @NotNull(message = "Indicator type cannot be null")
        RiskIndicator.IndicatorType type,
        @PositiveOrZero(message = "Threshold must be positive or zero")
        double threshold,
        @NotNull(message = "Threshold type cannot be null")
        RiskIndicator.ThresholdType thresholdType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}