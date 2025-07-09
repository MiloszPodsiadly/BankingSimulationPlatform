package com.milosz.podsiadly.domain.risk.dto;

import com.milosz.podsiadly.domain.risk.model.RiskAlert;
import com.milosz.podsiadly.domain.risk.model.RiskAssessment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record RiskReportDto(
        LocalDateTime reportGeneratedDate,
        LocalDateTime startDate,
        LocalDateTime endDate,
        long totalAssessments,
        Map<RiskAssessment.RiskLevel, Long> assessmentsByRiskLevel, // Liczba ocen dla każdego poziomu ryzyka
        List<RiskAssessmentDto> highRiskAssessments, // Lista ocen o wysokim ryzyku
        long totalAlerts,
        Map<RiskAlert.AlertStatus, Long> alertsByStatus, // Liczba alertów według statusu
        Map<RiskAssessment.RiskLevel, Long> alertsBySeverity, // Liczba alertów według poziomu ważności
        List<RiskAlertDto> openHighSeverityAlerts, // Otwarte alerty o wysokiej ważności
        List<RiskMetricDto> keyMetrics // Kluczowe metryki ryzyka
) {}