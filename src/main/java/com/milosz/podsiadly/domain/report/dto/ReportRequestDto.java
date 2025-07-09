package com.milosz.podsiadly.domain.report.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReportRequestDto(
        @NotNull(message = "Report type cannot be null")
        ReportType reportType,

        @NotNull(message = "Start date cannot be null")
        LocalDateTime startDate,

        @NotNull(message = "End date cannot be null")
        LocalDateTime endDate,

        // Optional: specific account ID, user ID, etc. for filtered reports
        Long accountId,
        Long userId
) {
    public enum ReportType {
        BALANCE_SHEET,
        PROFIT_AND_LOSS,
        AUDIT_SUMMARY,
        TRANSACTION_HISTORY,
        COMPLIANCE_ALERTS_SUMMARY,
        FINANCIAL_SUMMARY
    }
}
