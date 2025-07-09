package com.milosz.podsiadly.domain.compliance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "compliance_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class) // Enable JPA Auditing
public class ComplianceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String alertCode; // Unique code for the alert type (e.g., "SUS_TRAN_HIGH_AMOUNT")

    @Column(nullable = false)
    private String description; // Detailed description of the alert

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertSeverity severity; // Critical, High, Medium, Low

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertStatus status; // OPEN, RESOLVED, DISMISSED

    private String triggeredByEntityType; // e.g., "Transaction", "User"
    private Long triggeredByEntityId; // ID of the entity that triggered the alert

    private String relatedDetails; // Additional JSON or string details about the alert context

    @CreatedDate // Automatically set by JPA Auditing
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // When the alert was generated

    private LocalDateTime resolvedAt; // When the alert was resolved/dismissed

    private String resolvedBy; // User who resolved/dismissed the alert

    public enum AlertSeverity {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    public enum AlertStatus {
        OPEN,
        RESOLVED,
        DISMISSED
    }
}