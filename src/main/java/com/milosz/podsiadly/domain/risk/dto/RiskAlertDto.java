package com.milosz.podsiadly.domain.risk.dto;

import com.milosz.podsiadly.domain.risk.model.RiskAlert;
import com.milosz.podsiadly.domain.risk.model.RiskAssessment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

public record RiskAlertDto(
        Long id,
        @NotBlank(message = "Alert code cannot be blank")
        String alertCode,
        @NotBlank(message = "Description cannot be blank")
        String description,
        @NotNull(message = "Severity cannot be null")
        RiskAssessment.RiskLevel severity, // Severity z RiskAssessment.RiskLevel
        @NotNull(message = "Status cannot be null")
        RiskAlert.AlertStatus status,
        RiskAssessment.AssessmentEntityType triggeredByEntityType,
        Long triggeredByEntityId,
        Long relatedAssessmentId, // ID powiązanej oceny ryzyka
        String relatedDetails,
        @NotNull(message = "Created at date cannot be null")
        @PastOrPresent(message = "Created at date cannot be in the future")
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String resolvedBy
) {}