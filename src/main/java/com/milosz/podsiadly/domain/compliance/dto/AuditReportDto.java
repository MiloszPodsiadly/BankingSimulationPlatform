package com.milosz.podsiadly.domain.compliance.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AuditReportDto(
        LocalDateTime reportGeneratedAt,
        LocalDateTime reportPeriodStart,
        LocalDateTime reportPeriodEnd,
        Long totalAuditEntries,
        Long successfulEntries,
        Long failedEntries,
        Map<String, Long> entriesByActionType,
        List<AuditLogEntryDto> criticalFailedActions,
        List<AuditLogEntryDto> allEntriesInPeriod
) {}