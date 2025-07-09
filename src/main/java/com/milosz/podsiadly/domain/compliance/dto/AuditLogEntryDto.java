package com.milosz.podsiadly.domain.compliance.dto;

import com.milosz.podsiadly.domain.compliance.model.AuditLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AuditLogEntryDto(
        Long id,

        @NotBlank(message = "Username cannot be empty")
        String username,

        @NotBlank(message = "Action cannot be empty")
        String action,

        String entityType,
        Long entityId,
        String details,

        @NotNull(message = "Timestamp cannot be null")
        LocalDateTime timestamp,

        @NotNull(message = "Audit status cannot be null")
        AuditLog.AuditStatus status
) {}
