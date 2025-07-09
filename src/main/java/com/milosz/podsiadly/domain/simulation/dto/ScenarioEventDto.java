package com.milosz.podsiadly.domain.simulation.dto;

import com.milosz.podsiadly.domain.simulation.model.ScenarioEvent.EventType;
import com.milosz.podsiadly.domain.simulation.model.ScenarioEvent.RelatedEntityType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

// DTO jako rekord
public record ScenarioEventDto(
        Long id,
        @NotNull Long simulationRunId, // ID runu, do którego należy to zdarzenie
        @NotNull LocalDateTime eventTimestamp,
        @NotNull EventType eventType,
        String eventDetails, // JSON string or simple text for details
        RelatedEntityType relatedEntityType,
        Long relatedEntityId,
        Map<String, String> eventParameters
) {}