package com.milosz.podsiadly.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an event indicating that a transaction has failed.
 * This event is important for error handling, user notifications,
 * and logging failed attempts for auditing or security purposes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFailedEvent {

    /**
     * Unique identifier of the transaction that failed.
     * This might be a temporary ID or the original transaction ID if it was already persisted.
     */
    private Long transactionId;

    /**
     * Identifier of the source account involved in the transaction attempt.
     */
    private Long sourceAccountId;

    /**
     * Identifier of the target account involved in the transaction attempt.
     */
    private Long targetAccountId;

    /**
     * The amount of money that was attempted to be transferred.
     */
    private BigDecimal amount;

    /**
     * The currency of the transaction attempt.
     */
    private String currency;

    /**
     * The type of transaction that failed.
     */
    private String transactionType; // Możesz użyć Transaction.TransactionType, jeśli chcesz

    /**
     * The reason for the transaction failure (e.g., "INSUFFICIENT_FUNDS", "INVALID_ACCOUNT", "FRAUD_DETECTED").
     */
    private String reason;

    /**
     * Timestamp when the transaction failed (and when this event was generated).
     */
    private LocalDateTime failedAt;

    /**
     * Optional: Additional details about the failure.
     */
    private String details;

    /**
     * Identifier of the user who initiated the transaction (if applicable).
     */
    private Long userId; // Assuming a user is associated with the transaction
}