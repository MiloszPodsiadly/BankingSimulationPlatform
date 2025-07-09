package com.milosz.podsiadly.domain.risk.service;

import com.milosz.podsiadly.domain.risk.model.RiskAlert;
import com.milosz.podsiadly.domain.risk.model.RiskAssessment;
import com.milosz.podsiadly.domain.risk.model.RiskIndicator;
import com.milosz.podsiadly.domain.risk.repository.RiskAlertRepository;
import com.milosz.podsiadly.domain.risk.repository.RiskAssessmentRepository;
import com.milosz.podsiadly.domain.risk.repository.RiskIndicatorRepository;
import com.milosz.podsiadly.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskMonitoringService {

    private final RiskAlertRepository riskAlertRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskIndicatorRepository riskIndicatorRepository;

    /**
     * Monitors a new risk assessment and generates alerts if overall risk is high or critical.
     * @param assessment The newly created or updated risk assessment.
     * @return List of newly created alerts.
     */
    @Transactional
    public List<RiskAlert> monitorRiskAssessment(RiskAssessment assessment) {
        // Here you would check if the assessment's overallRiskLevel is above a certain threshold
        // and create a RiskAlert. This can also trigger notifications.
        if (assessment.getOverallRiskLevel() == RiskAssessment.RiskLevel.HIGH ||
                assessment.getOverallRiskLevel() == RiskAssessment.RiskLevel.CRITICAL) {

            log.warn("High or Critical Risk Assessment detected for entityType: {} ID: {}",
                    assessment.getEntityType(), assessment.getEntityId());

            RiskAlert alert = RiskAlert.builder()
                    .alertCode("OVERALL_RISK_ALERT")
                    .description("Overall risk level for " + assessment.getEntityType() + " ID " + assessment.getEntityId() + " is " + assessment.getOverallRiskLevel())
                    .severity(assessment.getOverallRiskLevel())
                    .status(RiskAlert.AlertStatus.OPEN)
                    .triggeredByEntityType(assessment.getEntityType())
                    .triggeredByEntityId(assessment.getEntityId())
                    .relatedAssessment(assessment)
                    .relatedDetails("Assessment Ref: " + assessment.getAssessmentRef())
                    .build();

            riskAlertRepository.save(alert);
            log.info("Generated risk alert: {}", alert.getAlertCode());
            return List.of(alert); // Return the generated alert
        }
        return List.of(); // No alerts generated
    }

    /**
     * Periodically checks all active indicators and recent assessments for potential alerts.
     * This method could be triggered by a scheduler.
     */
    @Transactional
    public void runPeriodicRiskChecks() {
        log.info("Running periodic risk checks...");
        List<RiskAssessment> recentAssessments = riskAssessmentRepository.findByAssessmentDateBetween(
                LocalDateTime.now().minusDays(7), LocalDateTime.now()); // Check assessments from last 7 days

        for (RiskAssessment assessment : recentAssessments) {
            monitorRiskAssessment(assessment); // Re-evaluate for overall risk alerts
            checkIndicatorThresholds(assessment); // Check individual indicators
        }
        log.info("Periodic risk checks completed.");
    }

    /**
     * Checks individual indicator values against their defined thresholds within an assessment.
     * @param assessment The risk assessment to check.
     * @return List of newly created alerts for specific indicator breaches.
     */
    @Transactional
    public List<RiskAlert> checkIndicatorThresholds(RiskAssessment assessment) {
        List<RiskAlert> generatedAlerts = new java.util.ArrayList<>();
        Map<String, BigDecimal> indicatorValues = assessment.getIndicatorValues();

        for (Map.Entry<String, BigDecimal> entry : indicatorValues.entrySet()) {
            String indicatorCode = entry.getKey();
            BigDecimal currentValue = entry.getValue();

            Optional<RiskIndicator> indicatorDefinitionOpt = riskIndicatorRepository.findByIndicatorCode(indicatorCode);

            if (indicatorDefinitionOpt.isPresent()) {
                RiskIndicator indicatorDefinition = indicatorDefinitionOpt.get();
                boolean thresholdBreached = false;

                switch (indicatorDefinition.getThresholdType()) {
                    case GREATER_THAN:
                        thresholdBreached = currentValue.compareTo(BigDecimal.valueOf(indicatorDefinition.getThreshold())) > 0;
                        break;
                    case LESS_THAN:
                        thresholdBreached = currentValue.compareTo(BigDecimal.valueOf(indicatorDefinition.getThreshold())) < 0;
                        break;
                    case GREATER_THAN_OR_EQUAL:
                        thresholdBreached = currentValue.compareTo(BigDecimal.valueOf(indicatorDefinition.getThreshold())) >= 0;
                        break;
                    case LESS_THAN_OR_EQUAL:
                        thresholdBreached = currentValue.compareTo(BigDecimal.valueOf(indicatorDefinition.getThreshold())) <= 0;
                        break;
                    case EQUALS:
                        thresholdBreached = currentValue.compareTo(BigDecimal.valueOf(indicatorDefinition.getThreshold())) == 0;
                        break;
                    case NOT_EQUALS:
                        thresholdBreached = currentValue.compareTo(BigDecimal.valueOf(indicatorDefinition.getThreshold())) != 0;
                        break;
                }

                if (thresholdBreached) {
                    log.warn("Risk indicator threshold breached: {} (Value: {}, Threshold: {}) for entityType: {} ID: {}",
                            indicatorCode, currentValue, indicatorDefinition.getThreshold(),
                            assessment.getEntityType(), assessment.getEntityId());

                    RiskAlert alert = RiskAlert.builder()
                            .alertCode("INDICATOR_BREACH_" + indicatorCode)
                            .description("Indicator '" + indicatorDefinition.getName() + "' value (" + currentValue + ") breached threshold (" + indicatorDefinition.getThreshold() + " " + indicatorDefinition.getThresholdType().name().replace("_", " ") + ")")
                            .severity(RiskAssessment.RiskLevel.HIGH) // Default to HIGH for indicator breaches
                            .status(RiskAlert.AlertStatus.OPEN)
                            .triggeredByEntityType(assessment.getEntityType())
                            .triggeredByEntityId(assessment.getEntityId())
                            .relatedAssessment(assessment)
                            .relatedDetails("Indicator: " + indicatorCode + ", Value: " + currentValue + ", Threshold: " + indicatorDefinition.getThreshold())
                            .build();
                    riskAlertRepository.save(alert);
                    generatedAlerts.add(alert);
                }
            }
        }
        return generatedAlerts;
    }

    @Transactional(readOnly = true)
    public List<RiskAlert> getOpenRiskAlerts() {
        return riskAlertRepository.findByStatus(RiskAlert.AlertStatus.OPEN);
    }

    @Transactional
    public RiskAlert resolveRiskAlert(Long alertId, String resolvedBy) {
        RiskAlert alert = riskAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Risk Alert not found with ID: " + alertId));
        alert.setStatus(RiskAlert.AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(resolvedBy);
        return riskAlertRepository.save(alert);
    }

    @Transactional
    public RiskAlert dismissRiskAlert(Long alertId, String dismissedBy) {
        RiskAlert alert = riskAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Risk Alert not found with ID: " + alertId));
        alert.setStatus(RiskAlert.AlertStatus.DISMISSED);
        alert.setResolvedAt(LocalDateTime.now()); // Treat as resolved date
        alert.setResolvedBy(dismissedBy);
        return riskAlertRepository.save(alert);
    }
}