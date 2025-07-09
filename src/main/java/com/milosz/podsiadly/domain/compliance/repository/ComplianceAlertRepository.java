package com.milosz.podsiadly.domain.compliance.repository;

import com.milosz.podsiadly.domain.compliance.model.ComplianceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplianceAlertRepository extends JpaRepository<ComplianceAlert, Long> {

    // Find alerts by their status (e.g., all OPEN alerts)
    List<ComplianceAlert> findByStatus(ComplianceAlert.AlertStatus status);

    // Find alerts by severity
    List<ComplianceAlert> findBySeverity(ComplianceAlert.AlertSeverity severity);

    // Find alerts triggered by a specific entity type and ID
    List<ComplianceAlert> findByTriggeredByEntityTypeAndTriggeredByEntityId(String entityType, Long entityId);

    // Find alerts generated within a specific time range
    List<ComplianceAlert> findByCreatedAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);

    // Find alerts by alert code
    List<ComplianceAlert> findByAlertCode(String alertCode);

    // Find open alerts with high or critical severity
    List<ComplianceAlert> findByStatusAndSeverityIn(ComplianceAlert.AlertStatus status, List<ComplianceAlert.AlertSeverity> severities);
}
