package com.milosz.podsiadly.domain.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransactionRequest(
        @NotNull(message = "Source account ID cannot be empty")
        Long sourceAccountId,
        @NotBlank(message = "Destination account number cannot be empty")
        String targetAccountNumber, // Używamy numeru konta, a nie ID, dla łatwości użycia przez klienta
        @NotNull(message = "Transaction amount cannot be empty")
        @Positive(message = "The transaction amount must be positive")
        BigDecimal amount,
        @NotBlank(message = "Transaction currency cannot be empty")
        String currency,
        String description
) {}