package com.milosz.podsiadly.domain.bank.dto;


import com.milosz.podsiadly.domain.bank.model.Deposit;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DepositDto(
        Long id,
        String depositNumber,
        Long accountId,
        String accountNumber, // Dodane dla czytelności
        BigDecimal amount,
        BigDecimal interestRate,
        Integer termMonths,
        LocalDate startDate,
        LocalDate endDate,
        Deposit.DepositStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
