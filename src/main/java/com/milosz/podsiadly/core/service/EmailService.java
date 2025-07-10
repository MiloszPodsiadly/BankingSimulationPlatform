package com.milosz.podsiadly.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for simulating email sending operations.
 * In a real-world application, this would integrate with an actual email sending provider
 * (e.g., SendGrid, Mailgun, SMTP server).
 */
@Service // Oznacza klasę jako komponent serwisowy Springa
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /**
     * Simulates sending a welcome email to a newly registered user or for a new account.
     *
     * @param userId The ID of the user to whom the email is sent.
     * @param accountNumber The account number related to the welcome email.
     */
    public void sendWelcomeEmail(Long userId, String accountNumber) {
        log.info("EmailService: Simulating sending welcome email to user ID: {} for account: {}", userId, accountNumber);
        // Tutaj w rzeczywistej aplikacji byłaby logika integracji z dostawcą poczty e-mail,
        // np. użycie JavaMailSender, SendGrid API, itp.
        // Na potrzeby symulacji po prostu logujemy akcję.
        log.info("Email sent: 'Welcome to Banking Simulation Platform!' to user ID: {}", userId);
    }

    /**
     * Simulates sending a welcome email to a new user upon registration.
     *
     * @param userId The ID of the newly registered user.
     * @param email The email address of the new user.
     */
    public void sendWelcomeEmailToNewUser(Long userId, String email) {
        log.info("EmailService: Simulating sending welcome email to new user ID: {}, email: {}", userId, email);
        // Logika wysyłki e-maila powitalnego po rejestracji użytkownika.
        log.info("Email sent: 'Welcome to Banking Simulation Platform!' to new user: {}", email);
    }

    // Możesz dodać więcej metod do wysyłania różnych typów e-maili, np.:
    /*
    public void sendTransactionConfirmationEmail(Long userId, Long transactionId, BigDecimal amount, String currency) {
        log.info("EmailService: Simulating sending transaction confirmation for user ID: {}, transaction ID: {}", userId, transactionId);
        // ...
    }

    public void sendPasswordResetEmail(Long userId, String email, String resetLink) {
        log.info("EmailService: Simulating sending password reset email to user ID: {}, email: {}", userId, email);
        // ...
    }
    */
}
