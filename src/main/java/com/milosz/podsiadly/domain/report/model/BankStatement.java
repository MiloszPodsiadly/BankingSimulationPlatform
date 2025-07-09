package com.milosz.podsiadly.domain.report.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "bank_statements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BankStatement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId; // The account for which the statement is generated

    @Column(nullable = false)
    private String statementReference; // Unique reference for the statement

    @Column(nullable = false)
    private LocalDateTime periodStart;

    @Column(nullable = false)
    private LocalDateTime periodEnd;

    @Column(nullable = false)
    private String statementType; // e.g., "MONTHLY", "ANNUAL", "CUSTOM"

    @Column(columnDefinition = "TEXT") // Store statement content as text (e.g., JSON, CSV, or path to file)
    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatementStatus status; // e.g., GENERATED, DELIVERED, FAILED

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime deliveredAt;

    public enum StatementStatus {
        GENERATED, DELIVERED, FAILED, PENDING
    }
}