package com.milosz.podsiadly.domain.scheduler;

import com.milosz.podsiadly.domain.simulation.service.BankingSimulationService; // Zakładam, że taki serwis powstanie
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimulationScheduler {

    private final BankingSimulationService bankingSimulationService; // Serwis do uruchamiania symulacji

    /**
     * Scheduled task to run banking simulations.
     * This example runs every 5 minutes.
     * The fixedRateString allows for configuration via application.properties if needed.
     * Example: @Scheduled(fixedRateString = "${simulation.scheduler.interval:300000}") // 5 minutes default
     */
    @Scheduled(fixedRate = 300000) // Runs every 5 minutes (300,000 milliseconds)
    public void runBankingSimulations() {
        log.info("Starting scheduled banking simulations at {}", System.currentTimeMillis());
        try {
            // Tutaj wywołamy główną metodę serwisu symulacji
            bankingSimulationService.runRandomBankingSimulation(); // Zakładam istnienie takiej metody
            log.info("Finished scheduled banking simulations successfully.");
        } catch (Exception e) {
            log.error("Error during scheduled banking simulations: {}", e.getMessage(), e);
            // Tutaj można dodać logikę powiadomień o błędach
        }
    }

    /**
     * Another example: run a daily risk assessment simulation.
     */
    @Scheduled(cron = "0 30 2 * * ?") // Runs daily at 02:30 AM
    public void runDailyRiskAssessmentSimulation() {
        log.info("Starting scheduled daily risk assessment simulation at {}", System.currentTimeMillis());
        try {
            // bankingSimulationService.runRiskAssessmentSimulation(); // Przykład innej metody
            log.info("Finished scheduled daily risk assessment simulation successfully.");
        } catch (Exception e) {
            log.error("Error during scheduled daily risk assessment simulation: {}", e.getMessage(), e);
        }
    }
}