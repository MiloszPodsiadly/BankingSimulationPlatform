package com.milosz.podsiadly.domain.compliance.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ComplianceReportDto(
        LocalDateTime reportGeneratedAt,
        LocalDateTime reportPeriodStart,
        LocalDateTime reportPeriodEnd,
        Long totalAlertsGenerated,
        Long openAlerts,
        Long resolvedAlerts,
        Long dismissedAlerts,
        Map<String, Long> alertsBySeverity,
        List<ComplianceAlertDto> openComplianceAlerts,
        Map<String, Long> ruleTriggerCounts
) {}