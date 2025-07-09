package com.milosz.podsiadly.domain.bank.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loanNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account; // Account to which the loan is linked

    @Column(nullable = false)
    private BigDecimal principalAmount; // Original amount of the loan

    @Column(nullable = false)
    private BigDecimal outstandingBalance; // Remaining balance to be paid

    @Column(nullable = false)
    private BigDecimal interestRate; // Annual interest rate (e.g., 0.05 for 5%)

    @Column(nullable = false)
    private Integer termMonths; // Loan term in months

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(nullable = false, updatable = false)
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

    public enum LoanStatus {
        ACTIVE, PAID_OFF, DEFAULTED, PENDING
    }
}
