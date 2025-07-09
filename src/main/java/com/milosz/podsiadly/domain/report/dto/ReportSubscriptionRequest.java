package com.milosz.podsiadly.domain.report.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportSubscriptionRequest(
        @NotBlank(message = "User ID cannot be empty")
        String userId,

        @NotNull(message = "Report request details cannot be null")
        @Valid // Ensure nested ReportRequestDto is also validated
        ReportRequestDto reportRequestDto,

        @NotBlank(message = "Frequency cannot be empty")
        String frequency, // e.g., "DAILY", "WEEKLY", "MONTHLY"

        @NotBlank(message = "Delivery method cannot be empty")
        String deliveryMethod // e.g., "EMAIL", "SFTP"
) {}