package com.milosz.podsiadly.domain.simulation.service;

import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.user.model.User;
import com.milosz.podsiadly.domain.bank.service.AccountService;
import com.milosz.podsiadly.domain.bank.service.TransactionService;
import com.milosz.podsiadly.domain.user.service.UserService;
import com.milosz.podsiadly.domain.risk.model.RiskAssessment;
import com.milosz.podsiadly.domain.risk.service.RiskCalculationService;
import com.milosz.podsiadly.domain.simulation.model.ScenarioEvent;
import com.milosz.podsiadly.domain.simulation.model.SimulationRun;
import com.milosz.podsiadly.domain.simulation.repository.ScenarioEventRepository; // Będzie potrzebne repozytorium dla ScenarioEvent
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationDataInjector {

    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final RiskCalculationService riskCalculationService;
    // Potrzebne repozytorium dla ScenarioEvent do zapisu wygenerowanych zdarzeń
    private final ScenarioEventRepository scenarioEventRepository; // Dodać to repozytorium!


    /**
     * Injects a list of generated simulation events into the main application domain.
     * This method processes each event and calls the appropriate service to "perform" it.
     *
     * @param simulationRun The current simulation run instance.
     * @param events The list of events to inject.
     * @return The number of events successfully injected.
     */
    @Transactional
    public int injectEvents(SimulationRun simulationRun, List<ScenarioEvent> events) {
        log.info("Injecting {} events for simulation run: {}", events.size(), simulationRun.getRunIdentifier());
        int injectedCount = 0;

        for (ScenarioEvent event : events) {
            try {
                // Associate event with the current simulation run before saving
                event.setSimulationRun(simulationRun);

                // Save the event to the database (for auditing/history)
                scenarioEventRepository.save(event);

                // Process the event based on its type
                switch (event.getEventType()) {
                    case ACCOUNT_CREATION:
                        // User and account creation is already handled by ScenarioGenerator,
                        // as it needs the generated user/account for subsequent transactions.
                        // Here, we just log that it was an injected event.
                        log.debug("Injected ACCOUNT_CREATION event: {}", event.getEventDetails());
                        break;
                    case TRANSACTION:
                        // Transactions are also created by ScenarioGenerator for linking.
                        // If ScenarioGenerator directly created Transaction entities, they are already persisted.
                        // Here, we just log.
                        log.debug("Injected TRANSACTION event: {}", event.getEventDetails());
                        break;
                    case FRAUD_ATTEMPT:
                        log.warn("Injected FRAUD_ATTEMPT event for account ID {}: {}", event.getRelatedEntityId(), event.getEventDetails());
                        // In a real scenario, this might trigger a specific fraud detection logic or risk alert directly.
                        // For now, it's logged and risk assessment might have been triggered by ScenarioGenerator.
                        break;
                    case RISK_ASSESSMENT_TRIGGER:
                        log.debug("Injected RISK_ASSESSMENT_TRIGGER event for entity ID {}: {}", event.getRelatedEntityId(), event.getEventDetails());
                        // Risk assessment is already performed by ScenarioGenerator, here we acknowledge it.
                        break;
                    case INTEREST_RATE_CHANGE:
                    case EXCHANGE_RATE_FLUCTUATION:
                    case NEWS_EVENT:
                        // These are more for contextual information or for other services to react to.
                        // The external data services have already cached them.
                        log.debug("Injected EXTERNAL_DATA_EVENT: {}", event.getEventDetails());
                        break;
                    // Add more event types as needed
                    case DEPOSIT:
                    case WITHDRAWAL:
                    case TRANSFER:
                        // If these are not handled by ScenarioGenerator directly as full transactions
                        // (i.e., if it only generated events and left injection to this service)
                        // Then here you would call transactionService.createDeposit/Withdrawal/Transfer
                        log.debug("Injected specific transaction type event: {}", event.getEventType());
                        break;
                    default:
                        log.warn("Unknown or unhandled event type during injection: {}", event.getEventType());
                        break;
                }
                injectedCount++;
            } catch (Exception e) {
                log.error("Failed to inject event {}: {}", event.getEventType(), e.getMessage(), e);
                // Continue with other events even if one fails
            }
        }
        log.info("Successfully injected {} out of {} events.", injectedCount, events.size());
        return injectedCount;
    }
}