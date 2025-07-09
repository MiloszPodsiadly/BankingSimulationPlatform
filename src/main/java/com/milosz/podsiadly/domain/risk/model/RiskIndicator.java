package com.milosz.podsiadly.domain.risk.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_indicators")
@Data // Generuje gettery, settery, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String indicatorCode; // Unique code for the indicator, e.g., "DEBT_TO_INCOME_RATIO"

    @Column(nullable = false)
    private String name; // Human-readable name, e.g., "Debt-to-Income Ratio"

    @Column(columnDefinition = "TEXT")
    private String description; // Detailed description of what the indicator represents

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IndicatorType type; // Type of indicator, e.g., FINANCIAL, OPERATIONAL, COMPLIANCE

    @Column(nullable = false)
    private double threshold; // Value beyond which the indicator is considered risky

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThresholdType thresholdType; // How the threshold is interpreted (e.g., GREATER_THAN, LESS_THAN)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum IndicatorType {
        FINANCIAL,
        OPERATIONAL,
        COMPLIANCE,
        CREDIT,
        MARKET,
        LIQUIDITY,
        STRATEGIC
    }

    public enum ThresholdType {
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN_OR_EQUAL,
        EQUALS,
        NOT_EQUALS
    }
}