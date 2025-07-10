package com.milosz.podsiadly.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map; // Do przechowywania zmiennych stanu symulacji

/**
 * Represents an event indicating the progression of a single step within the banking simulation.
 * This event is crucial for modules that need to react to the simulation's state changes,
 * such as real-time risk assessment, compliance monitoring, and performance tracking.
 */
@Data // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
@Builder // Provides a builder pattern for easy object creation
public class SimulationStepEvent {

    /**
     * Unique identifier for the entire simulation run.
     */
    private String simulationId;

    /**
     * The current step number within the simulation.
     */
    private Long stepNumber;

    /**
     * Timestamp when this simulation step occurred (and when this event was generated).
     */
    private LocalDateTime timestamp;

    /**
     * Optional: A map containing key-value pairs of simulation variables or metrics
     * relevant to this specific step (e.g., total transactions, active users, market volatility).
     */
    private Map<String, Object> metrics;

    /**
     * Optional: Description or type of the simulation step (e.g., "DAILY_TRANSACTIONS", "MONTHLY_INTEREST_CALCULATION").
     */
    private String stepType;

    // Możesz dodać więcej pól, jeśli są kluczowe dla tego zdarzenia,
    // np. ID użytkownika, który zainicjował krok symulacji,
    // czy konkretne ID transakcji, które zostały przetworzone w tym kroku.
}
