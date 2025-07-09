package com.milosz.podsiadly.domain.bank.dto;

import com.milosz.podsiadly.domain.bank.model.BankAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountDto(
        Long id,
        @NotBlank(message = "Account number cannot be empty")
        String accountNumber,
        @NotNull(message = "User Id cannot be empty")
        Long userId,
        @NotNull(message = "Balance cannot be empty")
        @PositiveOrZero(message = "The balance must be non-negative")
        BigDecimal balance,
        @NotBlank(message = "Currency cannot be empty") 
        String currency,
        @NotNull(message = "Account status cannot be empty")
        BankAccount.AccountStatus status,
        Long bankId, // ID banku, do którego należy konto
        String bankName, // Nazwa banku dla łatwiejszej prezentacji
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
