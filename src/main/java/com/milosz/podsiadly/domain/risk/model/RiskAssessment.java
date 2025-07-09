package com.milosz.podsiadly.domain.risk.model;

import com.milosz.podsiadly.domain.bank.model.BankAccount; // Zakładam, że BankAccount jest w pakiecie bank.model
import com.milosz.podsiadly.domain.user.model.User; // Zakładam, że User jest w pakiecie user.model

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "risk_assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String assessmentRef; // Unique reference for this assessment

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentEntityType entityType; // What is being assessed (e.g., ACCOUNT, USER, LOAN)

    @Column(nullable = false)
    private Long entityId; // ID of the entity being assessed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // Optional, if assessment is tied to a user
    private User assessedUser; // For user-specific assessments

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id") // Optional, if assessment is tied to an account
    private BankAccount assessedAccount; // For account-specific assessments

    @Column(nullable = false)
    private LocalDateTime assessmentDate; // When the assessment was performed

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel overallRiskLevel; // Overall calculated risk level (LOW, MEDIUM, HIGH, CRITICAL)

    @ElementCollection // Stores key-value pairs
    @CollectionTable(name = "risk_assessment_details", joinColumns = @JoinColumn(name = "assessment_id"))
    @MapKeyColumn(name = "indicator_code")
    @Column(name = "indicator_value")
    private Map<String, BigDecimal> indicatorValues; // Actual values for specific indicators (e.g., "DEBT_TO_INCOME_RATIO": 0.45)

    @Column(columnDefinition = "TEXT")
    private String notes; // Any additional notes or rationale for the assessment

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (assessmentRef == null) {
            assessmentRef = "RA-" + java.util.UUID.randomUUID().toString(); // Generate unique reference
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AssessmentEntityType {
        USER,
        ACCOUNT,
        LOAN,
        TRANSACTION,
        PRODUCT
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}