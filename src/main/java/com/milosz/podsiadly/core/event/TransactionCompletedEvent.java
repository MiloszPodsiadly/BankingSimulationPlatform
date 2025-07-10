package com.milosz.podsiadly.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an event indicating that a transaction has been successfully completed.
 * This event is crucial for various downstream processes like reporting,
 * risk assessment, and sending transaction confirmations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionCompletedEvent {

    /**
     * Unique identifier of the completed transaction.
     */
    private Long transactionId;

    /**
     * Identifier of the source account involved in the transaction.
     */
    private Long sourceAccountId;

    /**
     * Identifier of the target account involved in the transaction.
     */
    private Long targetAccountId;

    /**
     * The amount of money transferred in the transaction.
     */
    private BigDecimal amount;

    /**
     * The currency of the transaction (e.g., "PLN", "EUR", "USD").
     */
    private String currency;

    /**
     * The type of transaction (e.g., DEPOSIT, WITHDRAWAL, TRANSFER).
     */
    private String transactionType; // Możesz użyć Transaction.TransactionType, jeśli chcesz

    /**
     * Timestamp when the transaction was completed (and when this event was generated).
     */
    private LocalDateTime completedAt;

    /**
     * Optional: Description or purpose of the transaction.
     */
    private String description;

    /**
     * Identifier of the user who initiated the transaction (if applicable).
     */
    private Long userId; // Assuming a user is associated with the transaction

    // Możesz dodać więcej pól, jeśli są kluczowe dla tego zdarzenia.
}
