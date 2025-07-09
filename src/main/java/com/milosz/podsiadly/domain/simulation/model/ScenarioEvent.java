package com.milosz.podsiadly.domain.simulation.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "scenario_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private SimulationRun simulationRun;

    @Column(nullable = false)
    private LocalDateTime eventTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    private String eventDetails; // JSON string or simple text for details

    // If applicable, relate to domain entities by ID
    @Column(name = "related_entity_type")
    @Enumerated(EnumType.STRING)
    private RelatedEntityType relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    // Optional: parameters specific to this event
    @ElementCollection
    @CollectionTable(name = "event_parameters", joinColumns = @JoinColumn(name = "event_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value")
    private Map<String, String> eventParameters;

    public enum EventType {
        ACCOUNT_CREATION,
        TRANSACTION,
        USER_LOGIN,
        FRAUD_ATTEMPT,
        INTEREST_RATE_CHANGE,
        EXCHANGE_RATE_FLUCTUATION,
        NEWS_EVENT,
        RISK_ASSESSMENT_TRIGGER,
        ALERT_GENERATED,
        LOAN_APPLICATION,
        LOAN_APPROVAL,
        LOAN_REJECTION,
        LOAN_DEFAULT,
        DEPOSIT,
        WITHDRAWAL,
        TRANSFER
    }

    public enum RelatedEntityType {
        USER,
        ACCOUNT,
        TRANSACTION,
        LOAN,
        RISK_ASSESSMENT,
        ALERT,
        // Add more as needed
    }

    @PrePersist
    protected void onCreate() {
        if (this.eventTimestamp == null) {
            this.eventTimestamp = LocalDateTime.now();
        }
    }
}