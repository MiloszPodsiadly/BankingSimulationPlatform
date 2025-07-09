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
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class) // Enable JPA Auditing for automatic createdAt
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username; // User who performed the action

    @Column(nullable = false)
    private String action; // Description of the action (e.g., "DEPOSIT_CREATED", "ACCOUNT_ACCESSED")

    private String entityType; // Type of entity affected (e.g., "BankAccount", "Transaction")

    private Long entityId; // ID of the entity affected

    private String details; // Additional details in JSON or string format

    @CreatedDate // Automatically set by JPA Auditing
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp; // When the action occurred

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditStatus status; // Success or failure of the action

    public enum AuditStatus {
        SUCCESS,
        FAILURE
    }
}
