package com.milosz.podsiadly.domain.bank.processor;


import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.repository.TransactionRepository;
import com.milosz.podsiadly.core.event.TransactionCompletedEvent;
import com.milosz.podsiadly.core.event.TransactionFailedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Klasy eventowe (TransactionCompletedEvent, TransactionFailedEvent) muszą istnieć w com.milsoz.podsiadly.domain.core.events
// Poniżej znajdziesz ich proste definicje, jeśli ich jeszcze nie masz.

@Component
public class TransactionProcessor {

    private static final Logger log = LoggerFactory.getLogger(TransactionProcessor.class);
    private final TransactionRepository transactionRepository;

    public TransactionProcessor(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    // Ten procesor mógłby być używany do asynchronicznej walidacji, notyfikacji,
    // integracji z innymi systemami (np. systemem ryzyka, księgowości).

    @EventListener
    @Async // Opcjonalnie, aby zdarzenia były przetwarzane asynchronicznie
    @Transactional // Użyj transakcji, jeśli zmieniasz stan encji
    public void handleTransactionCompletedEvent(TransactionCompletedEvent event) {
        log.info("Otrzymano zdarzenie TransactionCompletedEvent dla transakcji ID: {}, Ref: {}", event.getTransactionId(), event.getTransactionRef());
        // Tutaj można by zaimplementować logikę biznesową, np.:
        // 1. Wysłać powiadomienie do użytkownika.
        // 2. Zaktualizować statystyki banku.
        // 3. Wysłać dane do systemu księgowego.
        // 4. Wywołać moduł zgodności (compliance).

        // Przykład: logowanie statusu w bazie danych, jeśli TransactionService tego nie zrobił
        transactionRepository.findById(event.getTransactionId()).ifPresent(transaction -> {
            if (transaction.getStatus() != Transaction.TransactionStatus.COMPLETED) {
                transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
                transactionRepository.save(transaction);
                log.info("Zaktualizowano status transakcji {} na COMPLETED.", event.getTransactionRef());
            }
        });
    }

    @EventListener
    @Async // Opcjonalnie
    @Transactional // Użyj transakcji, jeśli zmieniasz stan encji
    public void handleTransactionFailedEvent(TransactionFailedEvent event) {
        log.error("Otrzymano zdarzenie TransactionFailedEvent dla transakcji ID: {}, Ref: {}. Powód: {}", event.getTransactionId(), event.getTransactionRef(), event.getReason());
        // Tutaj można by zaimplementować logikę obsługi błędów, np.:
        // 1. Zapisanie szczegółów błędu do logów błędów.
        // 2. Powiadomienie administratorów.
        // 3. Cofnięcie częściowych operacji (jeśli nie obsłużono w @Transactional TransactionService).

        transactionRepository.findById(event.getTransactionId()).ifPresent(transaction -> {
            if (transaction.getStatus() != Transaction.TransactionStatus.FAILED) {
                transaction.setStatus(Transaction.TransactionStatus.FAILED);
                transaction.setDescription(transaction.getDescription() + " (Failed: " + event.getReason() + ")");
                transactionRepository.save(transaction);
                log.info("Zaktualizowano status transakcji {} na FAILED.", event.getTransactionRef());
            }
        });
    }
}