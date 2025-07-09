package com.milosz.podsiadly.domain.simulation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "simulation_scenarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String scenarioName;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScenarioType scenarioType;

    // Start time of the simulation relative to "now" or a specific date if fixed
    @Column(name = "start_date")
    private LocalDateTime startDate;

    // End time of the simulation
    @Column(name = "end_date")
    private LocalDateTime endDate;

    // Duration of the simulation in some unit (e.g., days, months)
    private Integer durationInDays;

    // Parameters for scenario generation (e.g., number of users, transactions per day, inflation rate fluctuation)
    @ElementCollection
    @CollectionTable(name = "scenario_parameters", joinColumns = @JoinColumn(name = "scenario_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value")
    private Map<String, String> parameters;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ScenarioType {
        DAILY_OPERATIONS,        // Regular banking operations
        MARKET_FLUCTUATION,      // Simulating market changes (e.g., interest rate changes, currency shifts)
        RISK_EVENT,              // Specific risk events (e.g., fraud attempts, large default)
        USER_BEHAVIOR_CHANGE,    // Changes in user spending/saving habits
        CUSTOM                   // For highly configurable scenarios
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}