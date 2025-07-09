package com.milosz.podsiadly.domain.simulation.dto;

import com.milosz.podsiadly.domain.simulation.model.SimulationRun.RunStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

// DTO jako rekord
public record SimulationRunStatusDto(
        @NotNull Long id,
        @NotBlank String runIdentifier,
        @NotNull Long scenarioId,
        @NotBlank String scenarioName, // Added for convenience
        @NotNull RunStatus status,
        @NotNull LocalDateTime startTime,
        LocalDateTime endTime,
        String resultSummary,
        Long generatedEventsCount
) {}