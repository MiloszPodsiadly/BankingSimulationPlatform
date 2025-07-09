package com.milosz.podsiadly.domain.simulation.repository;

import com.milosz.podsiadly.domain.simulation.model.SimulationRun;
import com.milosz.podsiadly.domain.simulation.model.SimulationRun.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SimulationRunRepository extends JpaRepository<SimulationRun, Long> {
    Optional<SimulationRun> findByRunIdentifier(String runIdentifier);
    List<SimulationRun> findByStatus(RunStatus status);
    List<SimulationRun> findBySimulationScenarioId(Long scenarioId);
    List<SimulationRun> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
}