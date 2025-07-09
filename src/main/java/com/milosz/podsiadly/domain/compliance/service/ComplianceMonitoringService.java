package com.milosz.podsiadly.domain.compliance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milosz.podsiadly.domain.compliance.model.AuditLog;
import com.milosz.podsiadly.domain.compliance.model.ComplianceAlert;
import com.milosz.podsiadly.domain.compliance.model.ComplianceAlert.AlertSeverity;
import com.milosz.podsiadly.domain.compliance.model.ComplianceAlert.AlertStatus;
import com.milosz.podsiadly.domain.compliance.repository.ComplianceAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j // For logging
public class ComplianceMonitoringService {

    private final ComplianceAlertRepository complianceAlertRepository;
    private final AuditService auditService; // To log events related to alerts
    private final ComplianceRuleEngine complianceRuleEngine; // To evaluate rules

    // A simple in-memory map to track suspicious activity per user for demo purposes
    // In a real system, this might be a distributed cache (Redis) or more sophisticated state management
    private final ConcurrentMap<String, Integer> userSuspiciousActivityCount = new ConcurrentHashMap<>();

    // Threshold for suspicious activity (e.g., 3 suspicious actions within a short period)
    private static final int SUSPICIOUS_ACTIVITY_THRESHOLD = 3;
    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("10000.00");

    /**
     * Processes an audit log entry to check for compliance violations and generate alerts.
     * This method would typically be called by a Kafka listener or directly from AuditService.
     * @param auditLog The audit log entry to process.
     */
    @Transactional
    public void processAuditLogForCompliance(AuditLog auditLog) {
        log.info("Processing audit log for compliance: Action={}, User={}", auditLog.getAction(), auditLog.getUsername());

        // Rule 1: Detect large transactions (example)
        if ("TRANSACTION_CREATED".equals(auditLog.getAction()) && auditLog.getDetails() != null) {
            try {
                // Assuming 'details' contains transaction amount
                // This part requires careful parsing based on your 'details' JSON structure
                // For now, let's assume details might contain "amount": 12345.67
                // In a real scenario, you'd parse JSON properly, e.g., using ObjectMapper
                if (auditLog.getDetails().contains("amount") && new BigDecimal(auditLog.getDetails().split("amount\":")[1].split(",")[0].trim()).compareTo(LARGE_TRANSACTION_THRESHOLD) > 0) {
                    generateAlert("LARGE_TRANSACTION", "Transaction of " + auditLog.getDetails() + " by " + auditLog.getUsername() + " exceeds threshold.",
                            AlertSeverity.HIGH, "Transaction", auditLog.getEntityId(), auditLog.getDetails());
                    auditService.logEvent(auditLog.getUsername(), "ALERT_GENERATED_LARGE_TRANSACTION", "ComplianceAlert", null, "Alert code: LARGE_TRANSACTION", AuditLog.AuditStatus.SUCCESS);
                }
            } catch (Exception e) {
                log.error("Failed to parse transaction amount from audit log details: {}", auditLog.getDetails(), e);
                auditService.logEvent(auditLog.getUsername(), "COMPLIANCE_ERROR_TRANSACTION_PARSE", "AuditLog", auditLog.getId(), "Error parsing transaction details: " + e.getMessage(), AuditLog.AuditStatus.FAILURE);
            }
        }

        // Rule 2: Detect multiple failed login attempts (example)
        if ("LOGIN_FAILURE".equals(auditLog.getAction())) {
            int count = userSuspiciousActivityCount.merge(auditLog.getUsername(), 1, Integer::sum);
            if (count >= SUSPICIOUS_ACTIVITY_THRESHOLD) {
                generateAlert("MULTIPLE_FAILED_LOGINS", "User " + auditLog.getUsername() + " has had " + count + " failed login attempts.",
                        AlertSeverity.CRITICAL, "User", null, "Username: " + auditLog.getUsername());
                userSuspiciousActivityCount.remove(auditLog.getUsername()); // Reset count after alert
                auditService.logEvent(auditLog.getUsername(), "ALERT_GENERATED_FAILED_LOGINS", "ComplianceAlert", null, "Alert code: MULTIPLE_FAILED_LOGINS", AuditLog.AuditStatus.SUCCESS);
            }
        }

        // Rule 3: Use ComplianceRuleEngine for more complex rules
        complianceRuleEngine.evaluateRules(auditLog).forEach(alert -> {
            generateAlert(alert.getAlertCode(), alert.getDescription(), alert.getSeverity(),
                    alert.getTriggeredByEntityType(), alert.getTriggeredByEntityId(), alert.getRelatedDetails());
            auditService.logEvent(auditLog.getUsername(), "ALERT_GENERATED_BY_RULE_ENGINE", "ComplianceAlert", null, "Alert code: " + alert.getAlertCode(), AuditLog.AuditStatus.SUCCESS);
        });
    }

    @Transactional
    public ComplianceAlert generateAlert(String alertCode, String description, AlertSeverity severity,
                                         String triggeredByEntityType, Long triggeredByEntityId, String relatedDetails) {
        ComplianceAlert alert = ComplianceAlert.builder()
                .alertCode(alertCode)
                .description(description)
                .severity(severity)
                .status(AlertStatus.OPEN)
                .triggeredByEntityType(triggeredByEntityType)
                .triggeredByEntityId(triggeredByEntityId)
                .relatedDetails(relatedDetails)
                .build();
        return complianceAlertRepository.save(alert);
    }

    /**
     * Resolves an open compliance alert.
     * @param alertId The ID of the alert to resolve.
     * @param resolvedBy The user who resolved the alert.
     * @return The resolved ComplianceAlert entity, or null if not found or already resolved.
     */
    @Transactional
    public ComplianceAlert resolveAlert(Long alertId, String resolvedBy) {
        return complianceAlertRepository.findById(alertId).map(alert -> {
            if (alert.getStatus() == AlertStatus.OPEN) {
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolvedAt(LocalDateTime.now());
                alert.setResolvedBy(resolvedBy);
                ComplianceAlert savedAlert = complianceAlertRepository.save(alert);
                auditService.logEvent(resolvedBy, "COMPLIANCE_ALERT_RESOLVED", "ComplianceAlert", alert.getId(), "Alert code: " + alert.getAlertCode(), AuditLog.AuditStatus.SUCCESS);
                return savedAlert;
            }
            return alert; // Already resolved or dismissed
        }).orElse(null);
    }

    /**
     * Dismisses an open compliance alert.
     * @param alertId The ID of the alert to dismiss.
     * @param dismissedBy The user who dismissed the alert.
     * @return The dismissed ComplianceAlert entity, or null if not found or already resolved.
     */
    @Transactional
    public ComplianceAlert dismissAlert(Long alertId, String dismissedBy) {
        return complianceAlertRepository.findById(alertId).map(alert -> {
            if (alert.getStatus() == AlertStatus.OPEN) {
                alert.setStatus(AlertStatus.DISMISSED);
                alert.setResolvedAt(LocalDateTime.now()); // Using resolvedAt for dismissal timestamp as well
                alert.setResolvedBy(dismissedBy);
                ComplianceAlert savedAlert = complianceAlertRepository.save(alert);
                auditService.logEvent(dismissedBy, "COMPLIANCE_ALERT_DISMISSED", "ComplianceAlert", alert.getId(), "Alert code: " + alert.getAlertCode(), AuditLog.AuditStatus.SUCCESS);
                return savedAlert;
            }
            return alert; // Already resolved or dismissed
        }).orElse(null);
    }

    /**
     * Retrieves all compliance alerts.
     * @return A list of all ComplianceAlert entries.
     */
    @Transactional(readOnly = true)
    public List<ComplianceAlert> getAllAlerts() {
        return complianceAlertRepository.findAll();
    }

    /**
     * Retrieves compliance alerts by status.
     * @param status The status to filter by.
     * @return A list of ComplianceAlert entries with the given status.
     */
    @Transactional(readOnly = true)
    public List<ComplianceAlert> getAlertsByStatus(AlertStatus status) {
        return complianceAlertRepository.findByStatus(status);
    }

    /**
     * Retrieves compliance alerts by severity.
     * @param severity The severity to filter by.
     * @return A list of ComplianceAlert entries with the given severity.
     */
    @Transactional(readOnly = true)
    public List<ComplianceAlert> getAlertsBySeverity(AlertSeverity severity) {
        return complianceAlertRepository.findBySeverity(severity);
    }
    public List<ComplianceAlert> getComplianceAlertsForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return complianceAlertRepository.findByCreatedAtBetween(startDate, endDate);
    }
}