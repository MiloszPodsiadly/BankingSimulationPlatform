package com.milosz.podsiadly.core.kafka.consumer;

import com.milosz.podsiadly.core.event.AccountCreatedEvent;
import com.milosz.podsiadly.core.event.TransactionCompletedEvent;
import com.milosz.podsiadly.core.event.TransactionFailedEvent;
import com.milosz.podsiadly.core.event.UserRegisteredEvent;
import com.milosz.podsiadly.core.service.EmailService;
import com.milosz.podsiadly.core.service.NotificationService;
import com.milosz.podsiadly.domain.report.service.DataAggregator; // ZMIENIONO: Import DataAggregator
import com.milosz.podsiadly.domain.risk.service.RiskAssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * General event listener and dispatcher.
 * This component receives various domain events from KafkaConsumerService
 * and dispatches them to appropriate business logic services for processing.
 * It acts as a central hub for event-driven architecture within the application.
 */
@Component
public class GeneralEventListener {

    private static final Logger log = LoggerFactory.getLogger(GeneralEventListener.class);

    // Wstrzykujemy serwisy, które będą reagować na zdarzenia
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final DataAggregator dataAggregator; // ZMIENIONO: Wstrzykujemy DataAggregator
    private final RiskAssessmentService riskAssessmentService;

    /**
     * Constructor for GeneralEventListener.
     * Spring will automatically inject all necessary services.
     *
     * @param emailService Service for sending emails.
     * @param notificationService Service for sending general notifications.
     * @param dataAggregator Service for handling data aggregation for reporting. // ZMIENIONO KOMENTARZ
     * @param riskAssessmentService Service for performing risk assessments.
     */
    public GeneralEventListener(EmailService emailService,
                                NotificationService notificationService,
                                DataAggregator dataAggregator, // ZMIENIONO PARAMETR
                                RiskAssessmentService riskAssessmentService) {
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.dataAggregator = dataAggregator; // ZMIENIONO PRZYPISANIE
        this.riskAssessmentService = riskAssessmentService;
    }

    /**
     * Handles AccountCreatedEvent.
     * Dispatches the event to relevant services for further processing.
     *
     * @param event The AccountCreatedEvent to handle.
     */
    public void handleAccountCreatedEvent(AccountCreatedEvent event) {
        log.info("GeneralEventListener: Handling AccountCreatedEvent for account ID: {}", event.getAccountId());

        // Przykładowa logika:
        // 1. Wyślij powiadomienie do użytkownika
        notificationService.sendAccountCreationNotification(event.getUserId(), event.getAccountId(), event.getAccountNumber());

        // 2. Wyślij e-mail powitalny
        emailService.sendWelcomeEmail(event.getUserId(), event.getAccountNumber());

        // 3. Zaktualizuj moduł raportowania (przekaż do DataAggregator)
        dataAggregator.processAccountCreationEvent(event); // ZMIENIONO: Wywołanie metody w DataAggregator

        // 4. Wykonaj wstępną ocenę ryzyka dla nowego konta (jeśli dotyczy)
        riskAssessmentService.assessNewAccountRisk(event.getAccountId(), event.getUserId());
    }

    /**
     * Handles TransactionCompletedEvent.
     * Dispatches the event to relevant services.
     *
     * @param event The TransactionCompletedEvent to handle.
     */
    public void handleTransactionCompletedEvent(TransactionCompletedEvent event) {
        log.info("GeneralEventListener: Handling TransactionCompletedEvent for transaction ID: {}", event.getTransactionId());
        // Przykład:
        notificationService.sendTransactionConfirmation(event.getUserId(), event.getTransactionId(), event.getAmount(), event.getCurrency());
        dataAggregator.processTransactionCompletedEvent(event); // ZMIENIONO: Wywołanie metody w DataAggregator
        riskAssessmentService.assessTransactionRisk(event.getTransactionId(), event.getAmount(), event.getCurrency());
    }

    /**
     * Handles TransactionFailedEvent.
     * Dispatches the event to relevant services.
     *
     * @param event The TransactionFailedEvent to handle.
     */
    public void handleTransactionFailedEvent(TransactionFailedEvent event) {
        log.warn("GeneralEventListener: Handling TransactionFailedEvent for transaction ID: {}. Reason: {}", event.getTransactionId(), event.getReason());
        // Przykład:
        notificationService.sendTransactionFailureNotification(event.getUserId(), event.getTransactionId(), event.getReason());
        dataAggregator.processTransactionFailedEvent(event); // ZMIENIONO: Wywołanie metody w DataAggregator
        // riskAssessmentService.logFailedTransactionAttempt(event.getTransactionId(), event.getReason()); // Możesz dodać taką metodę
    }

    /**
     * Handles UserRegisteredEvent.
     * Dispatches the event to relevant services.
     *
     * @param event The UserRegisteredEvent to handle.
     */
    public void handleUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("GeneralEventListener: Handling UserRegisteredEvent for user ID: {}", event.getUserId());
        // Przykład:
        emailService.sendWelcomeEmailToNewUser(event.getUserId(), event.getEmail());
        dataAggregator.processUserRegisteredEvent(event); // ZMIENIONO: Wywołanie metody w DataAggregator
    }
}
