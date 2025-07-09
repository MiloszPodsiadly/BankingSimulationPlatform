package com.milosz.podsiadly.domain.risk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "risk_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String alertCode; // Unique code for the alert, e.g., "HIGH_DEBT_RATIO"

    @Column(nullable = false)
    private String description; // Detailed description of the alert

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskAssessment.RiskLevel severity; // Severity of the alert (e.g., LOW, MEDIUM, HIGH, CRITICAL)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status; // Status of the alert (OPEN, RESOLVED, DISMISSED)

    @Enumerated(EnumType.STRING)
    private RiskAssessment.AssessmentEntityType triggeredByEntityType; // What type of entity triggered the alert
    private Long triggeredByEntityId; // ID of the entity that triggered the alert

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_assessment_id") // Optional, if tied to a specific assessment
    private RiskAssessment relatedAssessment; // Reference to the risk assessment that triggered this alert

    @Column(columnDefinition = "TEXT")
    private String relatedDetails; // JSON or string with additional relevant details

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt; // When the alert was resolved
    private String resolvedBy; // User who resolved the alert

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) { // Default status for new alerts
            status = AlertStatus.OPEN;
        }
    }

    public enum AlertStatus {
        OPEN,
        RESOLVED,
        DISMISSED
    }
}