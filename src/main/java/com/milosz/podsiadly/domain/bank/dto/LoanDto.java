package com.milosz.podsiadly.domain.bank.dto;

import com.milosz.podsiadly.domain.bank.model.Loan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoanDto(
        Long id,
        String loanNumber,
        Long accountId,
        String accountNumber, // Dodane dla czytelności
        BigDecimal principalAmount,
        BigDecimal outstandingBalance,
        BigDecimal interestRate,
        Integer termMonths,
        LocalDate startDate,
        LocalDate endDate,
        Loan.LoanStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}