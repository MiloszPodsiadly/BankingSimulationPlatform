package com.milosz.podsiadly.domain.compliance.dto;

import com.milosz.podsiadly.domain.compliance.model.ComplianceAlert;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ComplianceAlertDto(
        Long id,

        @NotBlank(message = "Alert code cannot be empty")
        String alertCode,

        @NotBlank(message = "Description cannot be empty")
        String description,

        @NotNull(message = "Severity cannot be null")
        ComplianceAlert.AlertSeverity severity,

        @NotNull(message = "Status cannot be null")
        ComplianceAlert.AlertStatus status,

        String triggeredByEntityType,
        Long triggeredByEntityId,
        String relatedDetails,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String resolvedBy
) {}