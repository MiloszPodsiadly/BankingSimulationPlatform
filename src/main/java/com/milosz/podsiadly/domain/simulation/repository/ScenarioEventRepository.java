package com.milosz.podsiadly.domain.simulation.repository;

import com.milosz.podsiadly.domain.simulation.model.ScenarioEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScenarioEventRepository extends JpaRepository<ScenarioEvent, Long> {
    List<ScenarioEvent> findBySimulationRunId(Long simulationRunId);
    List<ScenarioEvent> findByEventTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<ScenarioEvent> findByEventType(ScenarioEvent.EventType eventType);
}