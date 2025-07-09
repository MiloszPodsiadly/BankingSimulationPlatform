package com.milosz.podsiadly.domain.simulation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "simulation_runs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String runIdentifier; // Unique ID for each simulation run (e.g., UUID)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private SimulationScenario simulationScenario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    private String resultSummary; // Summary of the simulation outcome
    private String logs; // Reference to detailed logs if stored externally or short log here

    @Column(name = "generated_events_count")
    private Long generatedEventsCount; // Number of events generated during this run

    public enum RunStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        if (this.runIdentifier == null) {
            this.runIdentifier = UUID.randomUUID().toString();
        }
        this.startTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = RunStatus.PENDING;
        }
    }
}