package com.milosz.podsiadly.domain.risk.dto;

import com.milosz.podsiadly.domain.risk.model.RiskAssessment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record RiskAssessmentDto(
        Long id,
        @NotBlank(message = "Assessment reference cannot be blank")
        String assessmentRef,
        @NotNull(message = "Entity type cannot be null")
        RiskAssessment.AssessmentEntityType entityType,
        @NotNull(message = "Entity ID cannot be null")
        Long entityId,
        Long assessedUserId, // ID użytkownika (jeśli dotyczy)
        Long assessedAccountId, // ID konta (jeśli dotyczy)
        @NotNull(message = "Assessment date cannot be null")
        @PastOrPresent(message = "Assessment date cannot be in the future")
        LocalDateTime assessmentDate,
        @NotNull(message = "Overall risk level cannot be null")
        RiskAssessment.RiskLevel overallRiskLevel,
        Map<String, BigDecimal> indicatorValues, // Wartości dla poszczególnych wskaźników
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}