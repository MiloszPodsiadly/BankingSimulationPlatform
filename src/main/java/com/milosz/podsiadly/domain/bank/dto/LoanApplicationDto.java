package com.milosz.podsiadly.domain.bank.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record LoanApplicationDto(
        @NotNull(message = "Account ID cannot be empty")
        Long accountId,
        @NotNull(message = "Loan principal cannot be empty")
        @Positive(message = "The loan principal amount must be positive")
        BigDecimal principalAmount,
        @NotNull(message = "The interest rate cannot be empty")
        @Positive(message = "The interest rate must be positive")
        BigDecimal interestRate,
        @NotNull(message = "Loan period in months cannot be empty")
        @Positive(message = "The loan period must be positive")
        Integer termMonths
) {}
