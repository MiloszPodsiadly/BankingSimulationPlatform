package com.milosz.podsiadly.domain.risk.repository;

import com.milosz.podsiadly.domain.risk.model.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {
    Optional<RiskAssessment> findByAssessmentRef(String assessmentRef);
    List<RiskAssessment> findByEntityTypeAndEntityId(RiskAssessment.AssessmentEntityType entityType, Long entityId);
    List<RiskAssessment> findByOverallRiskLevel(RiskAssessment.RiskLevel riskLevel);
    List<RiskAssessment> findByAssessmentDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}