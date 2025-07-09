package com.milosz.podsiadly.domain.risk.service;

import com.milosz.podsiadly.domain.risk.dto.RiskAlertDto;
import com.milosz.podsiadly.domain.risk.dto.RiskAssessmentDto;
import com.milosz.podsiadly.domain.risk.dto.RiskMetricDto;
import com.milosz.podsiadly.domain.risk.dto.RiskReportDto;
import com.milosz.podsiadly.domain.risk.mapper.RiskMapper;
import com.milosz.podsiadly.domain.risk.model.RiskAlert;
import com.milosz.podsiadly.domain.risk.model.RiskAssessment;
import com.milosz.podsiadly.domain.risk.repository.RiskAlertRepository;
import com.milosz.podsiadly.domain.risk.repository.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskReportingService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskAlertRepository riskAlertRepository;
    private final RiskMapper riskMapper; // Wstrzykujemy RiskMapper

    @Transactional(readOnly = true)
    public RiskReportDto generateComprehensiveRiskReport(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating comprehensive risk report for period {} to {}", startDate, endDate);

        List<RiskAssessment> assessments = riskAssessmentRepository.findByAssessmentDateBetween(startDate, endDate);
        List<RiskAlert> alerts = riskAlertRepository.findByCreatedAtBetween(startDate, endDate);

        // Aggregate assessments by risk level
        Map<RiskAssessment.RiskLevel, Long> assessmentsByLevel = assessments.stream()
                .collect(Collectors.groupingBy(RiskAssessment::getOverallRiskLevel, Collectors.counting()));

        // Filter high risk assessments
        List<RiskAssessmentDto> highRiskAssessments = assessments.stream()
                .filter(a -> a.getOverallRiskLevel() == RiskAssessment.RiskLevel.HIGH ||
                        a.getOverallRiskLevel() == RiskAssessment.RiskLevel.CRITICAL)
                .map(riskMapper::toRiskAssessmentDto) // Używamy mappera
                .collect(Collectors.toList());

        // Aggregate alerts by status
        Map<RiskAlert.AlertStatus, Long> alertsByStatus = alerts.stream()
                .collect(Collectors.groupingBy(RiskAlert::getStatus, Collectors.counting()));

        // Aggregate alerts by severity
        Map<RiskAssessment.RiskLevel, Long> alertsBySeverity = alerts.stream()
                .collect(Collectors.groupingBy(RiskAlert::getSeverity, Collectors.counting()));

        // Filter open high severity alerts
        List<RiskAlertDto> openHighSeverityAlerts = alerts.stream()
                .filter(a -> a.getStatus() == RiskAlert.AlertStatus.OPEN &&
                        (a.getSeverity() == RiskAssessment.RiskLevel.HIGH ||
                                a.getSeverity() == RiskAssessment.RiskLevel.CRITICAL))
                .map(riskMapper::toRiskAlertDto) // Używamy mappera
                .collect(Collectors.toList());

        // Example key metrics (can be extended)
        List<RiskMetricDto> keyMetrics = List.of(
                new RiskMetricDto("Total Risk Assessments", BigDecimal.valueOf(assessments.size()), "count", LocalDateTime.now(), "Total number of risk assessments performed."),
                new RiskMetricDto("Total Risk Alerts", BigDecimal.valueOf(alerts.size()), "count", LocalDateTime.now(), "Total number of risk alerts generated."),
                new RiskMetricDto("Avg. Account Risk Level", calculateAverageAccountRiskLevel(assessments), "level", LocalDateTime.now(), "Average risk level across all accounts."),
                new RiskMetricDto("High Risk Accounts Count", BigDecimal.valueOf(assessments.stream().filter(a -> a.getOverallRiskLevel() == RiskAssessment.RiskLevel.HIGH || a.getOverallRiskLevel() == RiskAssessment.RiskLevel.CRITICAL).count()), "count", LocalDateTime.now(), "Number of accounts with high or critical risk.")
        );

        return new RiskReportDto(
                LocalDateTime.now(),
                startDate,
                endDate,
                assessments.size(),
                assessmentsByLevel,
                highRiskAssessments,
                alerts.size(),
                alertsByStatus,
                alertsBySeverity,
                openHighSeverityAlerts,
                keyMetrics
        );
    }

    private BigDecimal calculateAverageAccountRiskLevel(List<RiskAssessment> assessments) {
        // Simple average mapping LOW=1, MEDIUM=2, HIGH=3, CRITICAL=4
        long totalRiskPoints = 0;
        long totalAssessments = 0;

        for (RiskAssessment assessment : assessments) {
            if (assessment.getEntityType() == RiskAssessment.AssessmentEntityType.ACCOUNT) {
                totalAssessments++;
                switch (assessment.getOverallRiskLevel()) {
                    case LOW:       totalRiskPoints += 1; break;
                    case MEDIUM:    totalRiskPoints += 2; break;
                    case HIGH:      totalRiskPoints += 3; break;
                    case CRITICAL:  totalRiskPoints += 4; break;
                }
            }
        }
        if (totalAssessments == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf((double) totalRiskPoints / totalAssessments).setScale(2, RoundingMode.HALF_UP);
    }
}