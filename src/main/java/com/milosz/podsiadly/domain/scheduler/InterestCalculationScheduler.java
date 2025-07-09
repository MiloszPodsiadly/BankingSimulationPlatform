package com.milosz.podsiadly.domain.scheduler;

import com.milosz.podsiadly.domain.bank.service.AccountService; // Zakładam, że AccountService będzie miał metodę do naliczania odsetek
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterestCalculationScheduler {

    private final AccountService accountService; // Serwis odpowiedzialny za logikę kont bankowych

    /**
     * Scheduled task to calculate and apply interest to bank accounts.
     * This example runs daily at 00:00 (midnight).
     * The cron expression "0 0 0 * * ?" means:
     * - Second: 0
     * - Minute: 0
     * - Hour: 0 (midnight)
     * - Day of Month: * (every day)
     * - Month: * (every month)
     * - Day of Week: ? (no specific day of week)
     */
    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    public void calculateAndApplyInterest() {
        log.info("Starting scheduled interest calculation and application at {}", System.currentTimeMillis());
        try {
            // W realnym scenariuszu AccountService miałby metodę do obsługi naliczania odsetek dla wszystkich kont
            accountService.applyDailyInterest(); // Zakładam istnienie takiej metody
            log.info("Finished scheduled interest calculation and application successfully.");
        } catch (Exception e) {
            log.error("Error during scheduled interest calculation and application: {}", e.getMessage(), e);
            // Tutaj można dodać logikę powiadomień o błędach
        }
    }

    /**
     * Another example: run every first day of the month at 01:00 AM.
     * This could be for monthly interest statements or more complex calculations.
     */
    @Scheduled(cron = "0 0 1 1 * ?") // Runs on the 1st day of every month at 01:00 AM
    public void generateMonthlyInterestStatements() {
        log.info("Starting scheduled monthly interest statement generation at {}", System.currentTimeMillis());
        try {
            // accountService.generateMonthlyStatements(); // Przykład innej metody
            log.info("Finished scheduled monthly interest statement generation successfully.");
        } catch (Exception e) {
            log.error("Error during scheduled monthly interest statement generation: {}", e.getMessage(), e);
        }
    }
}