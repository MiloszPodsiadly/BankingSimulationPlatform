package com.milosz.podsiadly.domain.simulation.controller;

import com.milosz.podsiadly.domain.simulation.dto.ScenarioEventDto;
import com.milosz.podsiadly.domain.simulation.dto.SimulationConfigDto;
import com.milosz.podsiadly.domain.simulation.dto.SimulationRunStatusDto;
import com.milosz.podsiadly.domain.simulation.mapper.SimulationMapper;
import com.milosz.podsiadly.domain.simulation.model.SimulationRun;
import com.milosz.podsiadly.domain.simulation.model.SimulationScenario;
import com.milosz.podsiadly.domain.simulation.service.SimulationEngine;
import com.milosz.podsiadly.domain.simulation.repository.ScenarioEventRepository; // Potrzebne do pobierania zdarzeń
import com.milosz.podsiadly.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
@Slf4j
public class SimulationController {

    private final SimulationEngine simulationEngine;
    private final SimulationMapper simulationMapper;
    private final ScenarioEventRepository scenarioEventRepository; // Dodajemy repozytorium do pobierania zdarzeń

    // --- Endpoints for Simulation Scenarios ---

    /**
     * Creates a new simulation scenario.
     * POST /api/simulations/scenarios
     * @param configDto DTO containing scenario configuration.
     * @return The created SimulationScenario.
     */
    @PostMapping("/scenarios")
    public ResponseEntity<SimulationConfigDto> createScenario(@Valid @RequestBody SimulationConfigDto configDto) {
        log.info("Request to create new simulation scenario: {}", configDto.scenarioName());
        try {
            SimulationScenario scenario = simulationMapper.toSimulationScenarioEntity(configDto);
            SimulationScenario savedScenario = simulationEngine.saveScenario(scenario);
            // Zwracamy DTO, aby uniknąć problemów z rekurencyjnymi zależnościami JPA
            return new ResponseEntity<>(simulationMapper.toSimulationConfigDto(savedScenario), HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Error creating simulation scenario {}: {}", configDto.scenarioName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves all simulation scenarios.
     * GET /api/simulations/scenarios
     * @return A list of all simulation scenarios.
     */
    @GetMapping("/scenarios")
    public ResponseEntity<List<SimulationConfigDto>> getAllScenarios() {
        log.info("Request to get all simulation scenarios.");
        List<SimulationScenario> scenarios = simulationEngine.getAllScenarios();
        return ResponseEntity.ok(simulationMapper.toSimulationConfigDtoList(scenarios));
    }

    /**
     * Retrieves a simulation scenario by its ID.
     * GET /api/simulations/scenarios/{id}
     * @param id The ID of the scenario.
     * @return The SimulationScenario if found.
     */
    @GetMapping("/scenarios/{id}")
    public ResponseEntity<SimulationConfigDto> getScenarioById(@PathVariable Long id) {
        log.info("Request to get simulation scenario by ID: {}", id);
        return simulationEngine.getScenarioById(id)
                .map(simulationMapper::toSimulationConfigDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Deletes a simulation scenario by its ID.
     * DELETE /api/simulations/scenarios/{id}
     * @param id The ID of the scenario to delete.
     * @return 204 No Content if successful.
     */
    @DeleteMapping("/scenarios/{id}")
    public ResponseEntity<Void> deleteScenario(@PathVariable Long id) {
        log.info("Request to delete simulation scenario by ID: {}", id);
        try {
            simulationEngine.deleteScenario(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("Simulation scenario deletion failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error deleting simulation scenario ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // --- Endpoints for Simulation Runs ---

    /**
     * Starts a new simulation run based on a scenario ID.
     * POST /api/simulations/run/{scenarioId}
     * @param scenarioId The ID of the scenario to run.
     * @return The status of the initiated simulation run.
     */
    @PostMapping("/run/{scenarioId}")
    public ResponseEntity<SimulationRunStatusDto> startSimulation(@PathVariable Long scenarioId) {
        log.info("Request to start simulation for scenario ID: {}", scenarioId);
        try {
            SimulationRun simulationRun = simulationEngine.startSimulation(scenarioId);
            return new ResponseEntity<>(simulationMapper.toSimulationRunStatusDto(simulationRun), HttpStatus.ACCEPTED); // 202 Accepted
        } catch (ResourceNotFoundException e) {
            log.warn("Failed to start simulation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error starting simulation for scenario ID {}: {}", scenarioId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves the status of a specific simulation run.
     * GET /api/simulations/run/{runId}/status
     * @param runId The ID of the simulation run.
     * @return The status of the simulation run.
     */
    @GetMapping("/run/{runId}/status")
    public ResponseEntity<SimulationRunStatusDto> getSimulationRunStatus(@PathVariable Long runId) {
        log.info("Request to get status for simulation run ID: {}", runId);
        return simulationEngine.getSimulationRunStatus(runId)
                .map(simulationMapper::toSimulationRunStatusDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Retrieves all simulation runs.
     * GET /api/simulations/runs
     * @return A list of all simulation run statuses.
     */
    @GetMapping("/runs")
    public ResponseEntity<List<SimulationRunStatusDto>> getAllSimulationRuns() {
        log.info("Request to get all simulation runs.");
        List<SimulationRun> runs = simulationEngine.getAllSimulationRuns();
        return ResponseEntity.ok(simulationMapper.toSimulationRunStatusDtoList(runs));
    }

    /**
     * Cancels a running simulation.
     * PUT /api/simulations/run/{runIdentifier}/cancel
     * @param runIdentifier The unique identifier of the simulation run to cancel.
     * @return 200 OK if cancellation was attempted, 404 Not Found otherwise.
     */
    @PutMapping("/run/{runIdentifier}/cancel")
    public ResponseEntity<Void> cancelSimulation(@PathVariable String runIdentifier) {
        log.info("Request to cancel simulation run: {}", runIdentifier);
        if (simulationEngine.cancelSimulation(runIdentifier)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Retrieves all events generated during a specific simulation run.
     * GET /api/simulations/run/{runId}/events
     * @param runId The ID of the simulation run.
     * @return A list of scenario events.
     */
    @GetMapping("/run/{runId}/events")
    public ResponseEntity<List<ScenarioEventDto>> getSimulationRunEvents(@PathVariable Long runId) {
        log.info("Request to get events for simulation run ID: {}", runId);
        List<com.milosz.podsiadly.domain.simulation.model.ScenarioEvent> events = scenarioEventRepository.findBySimulationRunId(runId);
        return ResponseEntity.ok(simulationMapper.toScenarioEventDtoList(events));
    }

}