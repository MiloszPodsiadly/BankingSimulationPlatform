package com.milosz.podsiadly.domain.simulation.dto;

import com.milosz.podsiadly.domain.simulation.model.SimulationScenario.ScenarioType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.Map;

// DTO jako rekord
public record SimulationConfigDto(
        @NotBlank String scenarioName,
        @NotBlank String description,
        @NotNull ScenarioType scenarioType,
        LocalDateTime startDate,
        LocalDateTime endDate,
        @Positive Integer durationInDays, // Duration is required if start/end dates are not used
        Map<String, String> parameters // Configuration parameters for the simulation
) {}