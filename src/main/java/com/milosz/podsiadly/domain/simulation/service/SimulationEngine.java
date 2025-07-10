package com.milosz.podsiadly.domain.simulation.service;

import com.milosz.podsiadly.domain.simulation.model.ScenarioEvent;
import com.milosz.podsiadly.domain.simulation.model.SimulationRun;
import com.milosz.podsiadly.domain.simulation.model.SimulationRun.RunStatus;
import com.milosz.podsiadly.domain.simulation.model.SimulationScenario;
import com.milosz.podsiadly.domain.simulation.repository.SimulationRunRepository;
import com.milosz.podsiadly.domain.simulation.repository.SimulationScenarioRepository;
import com.milosz.podsiadly.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationEngine {

    private final SimulationRunRepository simulationRunRepository;
    private final SimulationScenarioRepository simulationScenarioRepository;
    private final ScenarioGenerator scenarioGenerator;
    private final SimulationDataInjector simulationDataInjector;

    // Use a fixed-size thread pool for running simulations to limit concurrent executions
    private final ExecutorService simulationExecutor = Executors.newFixedThreadPool(5); // Adjust pool size as needed
    // Store Future objects to manage running simulations (e.g., cancel, check status)
    private final ConcurrentHashMap<String, Future<?>> runningSimulations = new ConcurrentHashMap<>();


    /**
     * Starts a new simulation run based on a given scenario.
     * @param scenarioId The ID of the scenario to run.
     * @return The created SimulationRun entity.
     * @throws ResourceNotFoundException if the scenario is not found.
     */
    @Transactional
    public SimulationRun startSimulation(Long scenarioId) {
        SimulationScenario scenario = simulationScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException("SimulationScenario not found with ID: " + scenarioId));

        SimulationRun initialRun = SimulationRun.builder() // Use a new variable name for clarity
                .simulationScenario(scenario)
                .status(RunStatus.PENDING)
                .build();
        initialRun = simulationRunRepository.save(initialRun); // This is the final assignment to initialRun

        // Capture the ID of the saved run. This ID will be effectively final.
        final Long runId = initialRun.getId();
        final String runIdentifier = initialRun.getRunIdentifier(); // Also capture the identifier if needed

        log.info("Simulation run {} (Scenario: {}) created with status PENDING.", runIdentifier, scenario.getScenarioName());

        // Run the simulation in a separate thread to avoid blocking the API call
        Future<?> future = simulationExecutor.submit(() -> {
            try {
                // Fetch the SimulationRun again inside the async task to ensure it's managed
                // within the new transaction context and reflects any changes.
                SimulationRun currentRunInThread = simulationRunRepository.findById(runId)
                        .orElseThrow(() -> new IllegalStateException("SimulationRun not found after initiation."));

                currentRunInThread.setStatus(RunStatus.RUNNING);
                // No need to save here if the `processTransaction` or subsequent steps
                // will save it. But saving it here provides immediate status update.
                simulationRunRepository.save(currentRunInThread);
                log.info("Simulation run {} (Scenario: {}) is now RUNNING.", runIdentifier, scenario.getScenarioName());

                // 1. Generate events
                List<ScenarioEvent> generatedEvents = scenarioGenerator.generateEventsForScenario(currentRunInThread, scenario);
                currentRunInThread.setGeneratedEventsCount((long) generatedEvents.size());
                // simulationRunRepository.save(currentRunInThread); // Can save here or later

                // 2. Inject events into the core banking domain
                int injectedCount = simulationDataInjector.injectEvents(currentRunInThread, generatedEvents);

                // 3. Update simulation run status
                currentRunInThread.setStatus(RunStatus.COMPLETED);
                currentRunInThread.setEndTime(LocalDateTime.now());
                currentRunInThread.setResultSummary(String.format("Simulation completed. Generated %d events, injected %d.", generatedEvents.size(), injectedCount));
                simulationRunRepository.save(currentRunInThread); // Final save
                log.info("Simulation run {} (Scenario: {}) COMPLETED. Result: {}", runIdentifier, scenario.getScenarioName(), currentRunInThread.getResultSummary());

            } catch (Exception e) {
                log.error("Simulation run {} (Scenario: {}) FAILED: {}", runIdentifier, scenario.getScenarioName(), e.getMessage(), e);
                // Update status to FAILED
                simulationRunRepository.findById(runId).ifPresent(failedRun -> {
                    failedRun.setStatus(RunStatus.FAILED);
                    failedRun.setEndTime(LocalDateTime.now());
                    failedRun.setResultSummary("Simulation failed: " + e.getMessage());
                    simulationRunRepository.save(failedRun);
                });
            } finally {
                runningSimulations.remove(runIdentifier); // Remove from active list
            }
        });
        runningSimulations.put(runIdentifier, future); // Keep track of the future

        return initialRun; // Return the initialRun instance which is now saved and has an ID
    }

    /**
     * Gets the status of a specific simulation run.
     * @param runId The ID of the simulation run.
     * @return An Optional containing the SimulationRun entity.
     */
    @Transactional(readOnly = true)
    public Optional<SimulationRun> getSimulationRunStatus(Long runId) {
        return simulationRunRepository.findById(runId);
    }

    /**
     * Gets all simulation runs.
     * @return A list of all SimulationRun entities.
     */
    @Transactional(readOnly = true)
    public List<SimulationRun> getAllSimulationRuns() {
        return simulationRunRepository.findAll();
    }

    /**
     * Cancels a running simulation.
     * @param runIdentifier The unique identifier of the simulation run.
     * @return True if cancellation was attempted, false if not found or already completed.
     */
    public boolean cancelSimulation(String runIdentifier) {
        Future<?> future = runningSimulations.get(runIdentifier);
        if (future != null && !future.isDone() && !future.isCancelled()) {
            boolean cancelled = future.cancel(true); // Attempt to interrupt and cancel
            if (cancelled) {
                log.info("Attempted to cancel simulation run: {}", runIdentifier);
                // Update DB status asynchronously
                simulationRunRepository.findByRunIdentifier(runIdentifier).ifPresent(run -> {
                    run.setStatus(RunStatus.CANCELLED);
                    run.setEndTime(LocalDateTime.now());
                    run.setResultSummary("Simulation cancelled by user.");
                    simulationRunRepository.save(run);
                });
            }
            return cancelled;
        }
        log.warn("Cannot cancel simulation run {}: Not found or already finished.", runIdentifier);
        return false;
    }

    /**
     * Finds a simulation scenario by its name.
     * @param scenarioName The name of the scenario.
     * @return An Optional containing the SimulationScenario.
     */
    @Transactional(readOnly = true)
    public Optional<SimulationScenario> findScenarioByName(String scenarioName) {
        return simulationScenarioRepository.findByScenarioName(scenarioName);
    }

    /**
     * Saves or updates a simulation scenario.
     * @param scenario The scenario entity to save.
     * @return The saved scenario.
     */
    @Transactional
    public SimulationScenario saveScenario(SimulationScenario scenario) {
        if (scenario.getId() == null) {
            scenario.setCreatedAt(LocalDateTime.now());
        }
        scenario.setUpdatedAt(LocalDateTime.now());
        return simulationScenarioRepository.save(scenario);
    }

    /**
     * Retrieves all simulation scenarios.
     * @return A list of all simulation scenarios.
     */
    @Transactional(readOnly = true)
    public List<SimulationScenario> getAllScenarios() {
        return simulationScenarioRepository.findAll();
    }

    /**
     * Finds a simulation scenario by its ID.
     * @param scenarioId The ID of the scenario.
     * @return An Optional containing the SimulationScenario.
     */
    @Transactional(readOnly = true)
    public Optional<SimulationScenario> getScenarioById(Long scenarioId) {
        return simulationScenarioRepository.findById(scenarioId);
    }

    /**
     * Deletes a simulation scenario by its ID.
     * @param scenarioId The ID of the scenario to delete.
     */
    @Transactional
    public void deleteScenario(Long scenarioId) {
        if (!simulationScenarioRepository.existsById(scenarioId)) {
            throw new ResourceNotFoundException("SimulationScenario not found with ID: " + scenarioId);
        }
        simulationScenarioRepository.deleteById(scenarioId);
        log.info("Simulation scenario with ID {} deleted.", scenarioId);
    }
}