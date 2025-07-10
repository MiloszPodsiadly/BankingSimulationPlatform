package com.milosz.podsiadly.domain.simulation.service;

import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.user.model.User;
import com.milosz.podsiadly.domain.bank.service.AccountService;
import com.milosz.podsiadly.domain.bank.service.TransactionService;
import com.milosz.podsiadly.domain.user.service.UserService;
import com.milosz.podsiadly.domain.risk.service.RiskCalculationService;
import com.milosz.podsiadly.domain.simulation.data.service.EconomicDataService;
import com.milosz.podsiadly.domain.simulation.data.service.ExchangeRateService;
import com.milosz.podsiadly.domain.simulation.data.service.FinancialNewsService;
import com.milosz.podsiadly.domain.simulation.model.ScenarioEvent;
import com.milosz.podsiadly.domain.simulation.model.ScenarioEvent.EventType;
import com.milosz.podsiadly.domain.simulation.model.ScenarioEvent.RelatedEntityType;
import com.milosz.podsiadly.domain.simulation.model.SimulationRun;
import com.milosz.podsiadly.domain.simulation.model.SimulationScenario;
import com.milosz.podsiadly.domain.bank.service.BankService; // Import BankService
import jakarta.annotation.PostConstruct; // Import for @PostConstruct
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScenarioGenerator {

    private final UserService userService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final RiskCalculationService riskCalculationService; // Do generowania zdarzeń ryzyka
    private final ExchangeRateService exchangeRateService;
    private final FinancialNewsService financialNewsService;
    private final EconomicDataService economicDataService;
    private final BankService bankService; // Inject BankService to get default bank ID

    private final Random random = ThreadLocalRandom.current();

    private Long defaultBankId; // To store the ID of the bank for new accounts
    private String defaultAccountType = "CHECKING"; // Default type for simulation accounts

    /**
     * Initializes the default bank ID for simulations.
     * This method runs after dependency injection is complete.
     */
    @PostConstruct
    public void init() {
        // Try to get an existing bank or create one if none exists
        List<com.milosz.podsiadly.domain.bank.model.Bank> banks = bankService.getAllBanks();
        if (banks.isEmpty()) {
            log.warn("No banks found in the system. Creating a default bank for simulation purposes.");
            com.milosz.podsiadly.domain.bank.model.Bank defaultBank = com.milosz.podsiadly.domain.bank.model.Bank.builder()
                    .name("Simulated Bank PLC")
                    .bic("SIMBPLCC") // Example BIC, ensure it's unique if validated
                    .address("Warsaw, Poland")
                    .contactEmail("contact@simbank.com")
                    .build();
            try {
                defaultBank = bankService.createBank(defaultBank);
                this.defaultBankId = defaultBank.getId();
                log.info("Created default bank with ID: {}", this.defaultBankId);
            } catch (IllegalArgumentException e) {
                log.error("Failed to create default bank (might already exist by BIC): {}", e.getMessage());
                // Attempt to find it if it failed due to existing BIC
                this.defaultBankId = bankService.getBankByBic("SIMBPLCC")
                        .orElseThrow(() -> new IllegalStateException("Could not find or create default bank."))
                        .getId();
            }
        } else {
            this.defaultBankId = banks.get(0).getId(); // Use the first available bank
            log.info("Using existing bank with ID: {} for simulations.", this.defaultBankId);
        }
    }


    /**
     * Generates a series of events based on the given simulation scenario.
     * This method orchestrates the creation of various banking events.
     *
     * @param simulationRun The current simulation run instance.
     * @param scenario The simulation scenario definition.
     * @return A list of generated ScenarioEvent objects.
     */
    @Transactional
    public List<ScenarioEvent> generateEventsForScenario(SimulationRun simulationRun, SimulationScenario scenario) {
        log.info("Generating events for scenario: {} (Run ID: {})", scenario.getScenarioName(), simulationRun.getId());
        List<ScenarioEvent> generatedEvents = new ArrayList<>();

        LocalDateTime currentSimTime = scenario.getStartDate() != null ? scenario.getStartDate() : simulationRun.getStartTime();
        LocalDateTime endTime = scenario.getEndDate() != null ? scenario.getEndDate() : currentSimTime.plusDays(scenario.getDurationInDays());

        while (currentSimTime.isBefore(endTime)) {
            // Simulate daily activities
            generateDailyBankingEvents(simulationRun, currentSimTime, generatedEvents, scenario.getParameters());
            // Simulate external data events (e.g., news, rate changes based on cached data)
            generateExternalDataEvents(simulationRun, currentSimTime, generatedEvents, scenario.getParameters());
            // Simulate risk events based on a probability
            generateRiskEvents(simulationRun, currentSimTime, generatedEvents, scenario.getParameters());

            // Advance simulation time by a day or a random small interval
            currentSimTime = currentSimTime.plusDays(1); // Or plusHours(random.nextInt(24))
        }

        log.info("Finished generating {} events for scenario: {}", generatedEvents.size(), scenario.getScenarioName());
        return generatedEvents;
    }

    private void generateDailyBankingEvents(SimulationRun simulationRun, LocalDateTime eventTime,
                                            List<ScenarioEvent> events, Map<String, String> parameters) {
        int numUsersToCreate = Integer.parseInt(parameters.getOrDefault("numUsersPerDay", "1"));
        int numTransactionsPerUser = Integer.parseInt(parameters.getOrDefault("numTransactionsPerUser", "5"));

        for (int i = 0; i < numUsersToCreate; i++) {
            User newUser = generateUserAndAccount(eventTime);
            if (newUser != null) { // Ensure user and account were created successfully
                events.add(createScenarioEvent(simulationRun, eventTime, EventType.ACCOUNT_CREATION,
                        "New user and account created: " + newUser.getUsername(), RelatedEntityType.USER, newUser.getId()));

                // Fetch accounts for the new user, as createBankAccount returns the account, but User might not be immediately updated
                List<BankAccount> userAccounts = accountService.getAccountsByUserId(newUser.getId());

                if (!userAccounts.isEmpty()) {
                    BankAccount userAccount = userAccounts.get(0); // Use the first account
                    for (int j = 0; j < numTransactionsPerUser; j++) {
                        Transaction transaction = generateRandomTransaction(userAccount, eventTime);
                        if (transaction != null) {
                            events.add(createScenarioEvent(simulationRun, eventTime, EventType.TRANSACTION,
                                    "Transaction generated: " + transaction.getAmount(), RelatedEntityType.TRANSACTION, transaction.getId()));
                        }
                    }
                } else {
                    log.warn("No bank accounts found for newly created user {}. Skipping transactions.", newUser.getUsername());
                }
            }
        }
    }

    private User generateUserAndAccount(LocalDateTime eventTime) {
        try {
            String username = "sim_user_" + UUID.randomUUID().toString().substring(0, 8);
            String email = username + "@example.com";
            // --- CHANGE STARTS HERE ---
            User newUser = userService.createSimulationUser(username, email, "password123");
            // --- CHANGE ENDS HERE ---
            log.debug("Created simulated user: {}", newUser.getUsername());

            // --- CORRECTED CALL TO createBankAccount ---
            // accountService.createBankAccount(userId, bankId, accountType, currency, username);
            BankAccount newBankAccount = accountService.createBankAccount(
                    newUser.getId(),
                    defaultBankId,
                    defaultAccountType,
                    "PLN",
                    newUser.getUsername()
            );

            // Simulate an initial deposit to give the account a starting balance
            BigDecimal initialSimulationDeposit = BigDecimal.valueOf(random.nextDouble() * 1000 + 100)
                    .setScale(2, RoundingMode.HALF_UP);
            transactionService.createDepositTransaction(
                    newBankAccount.getId(),
                    initialSimulationDeposit,
                    "PLN",
                    "Initial Simulation Deposit",
                    eventTime
            );

            // It's good practice to ensure the user object has the account if needed for further processing
            // This assumes User model has a collection of BankAccounts and it's properly managed (e.g., @OneToMany with cascade)
            // If not, fetching userAccounts later (as done in generateDailyBankingEvents) is the way.
            if (newUser.getBankAccounts() == null) {
                newUser.setBankAccounts(new ArrayList<>());
            }
            newUser.getBankAccounts().add(newBankAccount); // Add the created account to the user's collection

            log.debug("Created account {} with initial balance {} for user: {}", newBankAccount.getAccountNumber(), initialSimulationDeposit, newUser.getUsername());
            return newUser;
        } catch (Exception e) {
            log.error("Failed to generate user and account during simulation: {}", e.getMessage(), e); // Log full stack trace
            return null;
        }
    }

    private Transaction generateRandomTransaction(BankAccount sourceAccount, LocalDateTime eventTime) {
        try {
            // Get a random existing account to be the target
            List<BankAccount> allAccounts = accountService.getAllBankAccounts();
            if (allAccounts.isEmpty()) {
                log.warn("No other accounts available for transactions.");
                return null;
            }
            BankAccount targetAccount = allAccounts.get(random.nextInt(allAccounts.size()));

            if (sourceAccount.getId().equals(targetAccount.getId())) { // Avoid self-transfer
                if (allAccounts.size() == 1) {
                    log.debug("Only one account exists. Cannot perform self-transfer.");
                    return null; // No other accounts to transfer to
                }
                // Try to find a different account if current target is same as source
                Optional<BankAccount> differentAccount = allAccounts.stream()
                        .filter(acc -> !acc.getId().equals(sourceAccount.getId()))
                        .findAny(); // Find any other account
                if (differentAccount.isPresent()) {
                    targetAccount = differentAccount.get();
                } else {
                    log.debug("Could not find a different account for transfer. Skipping.");
                    return null; // Still no other accounts
                }
            }

            BigDecimal amount = BigDecimal.valueOf(random.nextDouble() * 500 + 10) // Random amount between 10 and 510
                    .setScale(2, RoundingMode.HALF_UP);

            if (sourceAccount.getBalance().compareTo(amount) < 0) {
                log.debug("Insufficient funds ({}) for transaction of {} from account {}. Skipping.",
                        sourceAccount.getBalance(), amount, sourceAccount.getAccountNumber());
                return null;
            }

            // --- CORRECTED CALL TO createTransferTransaction ---
            // createTransferTransaction(Long sourceAccountId, String targetAccountNumber, BigDecimal amount, String currency, String description, LocalDateTime transactionDateTime)
            return transactionService.createTransferTransaction(
                    sourceAccount.getId(),
                    targetAccount.getAccountNumber(),
                    amount,
                    sourceAccount.getCurrency(), // Use source account's currency
                    "Simulated Transfer",
                    eventTime
            );
        } catch (Exception e) {
            log.error("Failed to generate random transaction during simulation: {}", e.getMessage(), e); // Log full stack trace
            return null;
        }
    }

    private void generateExternalDataEvents(SimulationRun simulationRun, LocalDateTime eventTime,
                                            List<ScenarioEvent> events, Map<String, String> parameters) {
        // Example: Periodically log the latest exchange rates or news
        if (random.nextDouble() < 0.2) { // 20% chance per simulation "day"
            // Note: exchangeRateService.lastBaseCurrency might be null if not set, consider a default
            String baseCurrency = exchangeRateService.getLastBaseCurrency() != null ? exchangeRateService.getLastBaseCurrency() : "PLN";
            exchangeRateService.getLatestExchangeRates().forEach((currency, rate) ->
                    events.add(createScenarioEvent(simulationRun, eventTime, EventType.EXCHANGE_RATE_FLUCTUATION,
                            String.format("Exchange rate %s/%s: %s", baseCurrency, currency, rate),
                            null, null)));
        }

        if (random.nextDouble() < 0.1) { // 10% chance
            financialNewsService.getLatestNews().stream().findFirst().ifPresent(article ->
                    events.add(createScenarioEvent(simulationRun, eventTime, EventType.NEWS_EVENT,
                            "Financial News: " + article.title(), null, null)));
        }

        if (random.nextDouble() < 0.05) { // 5% chance
            economicDataService.getCachedIndicatorValue("PL", "FP.CPI.TOTL.ZG").ifPresent(inflation ->
                    events.add(createScenarioEvent(simulationRun, eventTime, EventType.INTEREST_RATE_CHANGE, // Reusing event type for economic impact
                            "Poland Inflation (CPI): " + inflation + "%", null, null)));
        }
    }

    private void generateRiskEvents(SimulationRun simulationRun, LocalDateTime eventTime,
                                    List<ScenarioEvent> events, Map<String, String> parameters) {
        double fraudProbability = Double.parseDouble(parameters.getOrDefault("fraudProbability", "0.01"));
        if (random.nextDouble() < fraudProbability) {
            // Simulate a fraud attempt on a random account
            List<BankAccount> accounts = accountService.getAllBankAccounts();
            if (!accounts.isEmpty()) {
                BankAccount targetAccount = accounts.get(random.nextInt(accounts.size()));
                events.add(createScenarioEvent(simulationRun, eventTime, EventType.FRAUD_ATTEMPT,
                        "Simulated fraud attempt on account " + targetAccount.getAccountNumber(),
                        RelatedEntityType.ACCOUNT, targetAccount.getId()));
                log.warn("SIMULATION: Fraud attempt generated for account {}", targetAccount.getAccountNumber());

                // Trigger a risk assessment for the account that "experienced" fraud
                riskCalculationService.performAccountRiskAssessment(targetAccount.getId());
                events.add(createScenarioEvent(simulationRun, eventTime, EventType.RISK_ASSESSMENT_TRIGGER,
                        "Risk assessment triggered for account " + targetAccount.getAccountNumber() + " due to fraud attempt",
                        RelatedEntityType.ACCOUNT, targetAccount.getId()));
            }
        }
        // Other risk events like loan default, large withdrawal etc.
    }


    private ScenarioEvent createScenarioEvent(SimulationRun simulationRun, LocalDateTime eventTime, EventType type,
                                              String details, RelatedEntityType relatedType, Long relatedId) {
        return ScenarioEvent.builder()
                .simulationRun(simulationRun)
                .eventTimestamp(eventTime)
                .eventType(type)
                .eventDetails(details)
                .relatedEntityType(relatedType)
                .relatedEntityId(relatedId)
                .build();
    }
}