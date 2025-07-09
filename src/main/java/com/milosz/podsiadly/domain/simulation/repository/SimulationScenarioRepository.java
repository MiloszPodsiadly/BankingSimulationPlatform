package com.milosz.podsiadly.domain.simulation.repository;

import com.milosz.podsiadly.domain.simulation.model.SimulationScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SimulationScenarioRepository extends JpaRepository<SimulationScenario, Long> {
    Optional<SimulationScenario> findByScenarioName(String scenarioName);
}