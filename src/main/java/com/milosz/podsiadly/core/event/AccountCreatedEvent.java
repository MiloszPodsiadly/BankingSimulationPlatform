package com.milosz.podsiadly.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents an event indicating that a new bank account has been successfully created.
 * This event is crucial for asynchronous processing by other modules,
 * such as reporting, compliance, risk assessment, or notification services.
 *
 * It contains essential, lightweight information about the new account,
 * avoiding the transfer of full entity objects to keep Kafka messages efficient.
 */
@Data // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
@Builder // Provides a builder pattern for easy object creation
public class AccountCreatedEvent {

    /**
     * Unique identifier of the newly created bank account.
     */
    private Long accountId;

    /**
     * The unique account number (e.g., IBAN-like format).
     */
    private String accountNumber;

    /**
     * Identifier of the user to whom this account belongs.
     */
    private Long userId;

    /**
     * The initial balance of the account at the time of creation.
     */
    private BigDecimal initialBalance;

    /**
     * The currency of the account (e.g., "PLN", "EUR", "USD").
     */
    private String currency;

    /**
     * The type of account (e.g., "SAVINGS", "CHECKING", "LOAN").
     */
    private String accountType;

    /**
     * Timestamp when the account was created (and when this event was generated).
     */
    private LocalDateTime createdAt;

    // Możesz dodać więcej pól, jeśli są kluczowe dla tego zdarzenia,
    // ale staraj się, aby było ono jak najlżejsze.
}
