package com.milosz.podsiadly.domain.risk.repository;

import com.milosz.podsiadly.domain.risk.model.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {
    List<RiskAlert> findByStatus(RiskAlert.AlertStatus status);
    //List<RiskAlert> findBySeverity(RiskAlert.RiskLevel severity);
    //List<RiskAlert> findByTriggeredByEntityTypeAndTriggeredByEntityId(RiskAlert.AssessmentEntityType entityType, Long entityId);
    List<RiskAlert> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<RiskAlert> findByRelatedAssessmentId(Long assessmentId);
}