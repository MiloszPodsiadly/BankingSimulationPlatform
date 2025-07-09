package com.milosz.podsiadly.domain.bank.dto;

import com.milosz.podsiadly.domain.bank.model.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDto(
        Long id,
        String transactionRef,
        Long sourceAccountId,
        String sourceAccountNumber, // Dodane dla czytelności
        Long targetAccountId,
        String targetAccountNumber, // Dodane dla czytelności
        BigDecimal amount,
        String currency,
        Transaction.TransactionType type,
        Transaction.TransactionStatus status,
        String description,
        LocalDateTime transactionDate
) {}