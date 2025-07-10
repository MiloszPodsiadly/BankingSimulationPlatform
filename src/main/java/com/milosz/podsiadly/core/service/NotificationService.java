package com.milosz.podsiadly.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sending various types of notifications to users.
 * In a real-world application, this would integrate with different notification channels
 * such as in-app notifications, SMS, or push notifications.
 * For simulation purposes, actions are logged.
 */
@Service // Oznacza klasę jako komponent serwisowy Springa
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Simulates sending a notification about a newly created bank account.
     *
     * @param userId The ID of the user to notify.
     * @param accountId The ID of the newly created account.
     * @param accountNumber The account number of the newly created account.
     */
    public void sendAccountCreationNotification(Long userId, Long accountId, String accountNumber) {
        log.info("NotificationService: Simulating sending account creation notification to user ID: {} for account ID: {}, number: {}",
                userId, accountId, accountNumber);
        // Tutaj w rzeczywistej aplikacji byłaby logika wysyłania powiadomień,
        // np. przez WebSocket (dla powiadomień w aplikacji), SMS Gateway, FCM (dla push).
        log.info("Notification sent: 'Your account {} has been successfully created!' to user ID: {}", accountNumber, userId);
    }

    /**
     * Simulates sending a transaction confirmation notification.
     *
     * @param userId The ID of the user involved in the transaction.
     * @param transactionId The ID of the completed transaction.
     * @param amount The amount of the transaction.
     * @param currency The currency of the transaction.
     */
    public void sendTransactionConfirmation(Long userId, Long transactionId, java.math.BigDecimal amount, String currency) {
        log.info("NotificationService: Simulating sending transaction confirmation to user ID: {} for transaction ID: {}, amount: {} {}",
                userId, transactionId, amount, currency);
        // Logika wysyłki powiadomienia o potwierdzeniu transakcji.
        log.info("Notification sent: 'Transaction {} of {} {} completed!' to user ID: {}", transactionId, amount, currency, userId);
    }

    /**
     * Simulates sending a notification about a failed transaction.
     *
     * @param userId The ID of the user involved in the transaction.
     * @param transactionId The ID of the failed transaction.
     * @param reason The reason for the transaction failure.
     */
    public void sendTransactionFailureNotification(Long userId, Long transactionId, String reason) {
        log.warn("NotificationService: Simulating sending transaction failure notification to user ID: {} for transaction ID: {}. Reason: {}",
                userId, transactionId, reason);
        // Logika wysyłki powiadomienia o nieudanej transakcji.
        log.warn("Notification sent: 'Transaction {} failed! Reason: {}' to user ID: {}", transactionId, reason, userId);
    }

    // Możesz dodać więcej metod do wysyłania różnych typów powiadomień.
}
