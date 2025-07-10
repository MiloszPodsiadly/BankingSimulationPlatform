package com.milosz.podsiadly.domain.bank.service;

import com.milosz.podsiadly.common.exception.InsufficientFundsException;
import com.milosz.podsiadly.common.exception.ResourceNotFoundException;
import com.milosz.podsiadly.domain.bank.dto.TransactionRequest; // Nadal potrzebne, jeśli używasz toEntity z mappera
import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.repository.BankAccountRepository;
import com.milosz.podsiadly.domain.bank.repository.TransactionRepository;
import com.milosz.podsiadly.core.event.TransactionCompletedEvent;
import com.milosz.podsiadly.core.event.TransactionFailedEvent;
import com.milosz.podsiadly.core.kafka.producer.EventProducer;
import com.milosz.podsiadly.domain.compliance.model.AuditLog;
import com.milosz.podsiadly.domain.compliance.service.AuditService;
import com.milosz.podsiadly.domain.bank.mapper.TransactionMapper; // Potrzebne, jeśli używasz toEntity z mappera
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j // Lombok do logowania
@RequiredArgsConstructor // Lombok do generowania konstruktora z wymaganymi polami (final)
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final EventProducer eventProducer;
    private final AuditService auditService;
    private final TransactionMapper transactionMapper; // Pozostawiamy, jeśli jest używany (np. dla TransactionRequest)

    /**
     * Główna metoda do przetwarzania dowolnego typu transakcji.
     * Obsługuje walidację, aktualizację sald, persystencję transakcji
     * oraz publikowanie zdarzeń do Kafki.
     *
     * @param transaction Obiekt transakcji do przetworzenia.
     * @return Przetworzony i zapisany obiekt transakcji.
     * @throws IllegalArgumentException jeśli dane transakcji są nieprawidłowe.
     * @throws InsufficientFundsException jeśli konto źródłowe ma niewystarczające środki.
     * @throws ResourceNotFoundException jeśli konta nie zostaną znalezione.
     */
    @Transactional
    public Transaction processTransaction(Transaction transaction) {
        BankAccount sourceAccount = transaction.getSourceAccount();
        BankAccount targetAccount = transaction.getTargetAccount();
        BigDecimal amount = transaction.getAmount();

        // Podstawowe walidacje
        if (sourceAccount == null && targetAccount == null) {
            throw new IllegalArgumentException("Muszą być podane co najmniej jedno konto źródłowe lub docelowe.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota transakcji musi być dodatnia.");
        }
        if (sourceAccount != null && !sourceAccount.getCurrency().equals(transaction.getCurrency())) {
            throw new IllegalArgumentException("Waluta transakcji musi być zgodna z walutą konta źródłowego.");
        }
        if (targetAccount != null && !targetAccount.getCurrency().equals(transaction.getCurrency())) {
            throw new IllegalArgumentException("Waluta transakcji musi być zgodna z walutą konta docelowego.");
        }

        // Ustawienie referencji transakcji i początkowego statusu
        if (transaction.getTransactionRef() == null || transaction.getTransactionRef().isEmpty()) {
            transaction.setTransactionRef("TRN-" + UUID.randomUUID().toString());
        }
        transaction.setStatus(Transaction.TransactionStatus.PENDING); // Początkowy status
        if (transaction.getTransactionDate() == null) { // Ustaw datę, jeśli nie jest już ustawiona
            transaction.setTransactionDate(LocalDateTime.now());
        }

        try {
            // Obsługa różnych typów transakcji
            switch (transaction.getType()) {
                case TRANSFER:
                    handleTransfer(sourceAccount, targetAccount, amount);
                    break;
                case DEPOSIT:
                    handleDeposit(targetAccount, amount);
                    break;
                case WITHDRAWAL:
                    handleWithdrawal(sourceAccount, amount);
                    break;
                case LOAN_REPAYMENT:
                    handleLoanRepayment(sourceAccount, amount);
                    break;
                case INTEREST_PAYOUT:
                    handleInterestPayout(targetAccount, amount);
                    break;
                case FEE:
                    handleFee(sourceAccount, amount);
                    break;
                default:
                    throw new IllegalArgumentException("Nieznany typ transakcji: " + transaction.getType());
            }

            // Zapisz transakcję po pomyślnym przetworzeniu sald
            Transaction savedTransaction = transactionRepository.save(transaction);
            savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED); // Zmień status na COMPLETED
            transactionRepository.save(savedTransaction); // Zaktualizuj status w bazie

            log.info("Transaction completed successfully with ID: {}", savedTransaction.getId());

            // Publikacja zdarzenia TransactionCompletedEvent do KAFKI
            TransactionCompletedEvent completedEvent = TransactionCompletedEvent.builder()
                    .transactionId(savedTransaction.getId())
                    .sourceAccountId(savedTransaction.getSourceAccount() != null ? savedTransaction.getSourceAccount().getId() : null)
                    .targetAccountId(savedTransaction.getTargetAccount() != null ? savedTransaction.getTargetAccount().getId() : null)
                    .amount(savedTransaction.getAmount())
                    .currency(savedTransaction.getCurrency())
                    .transactionType(savedTransaction.getType().name())
                    .completedAt(savedTransaction.getTransactionDate())
                    .description(savedTransaction.getDescription())
                    .userId(savedTransaction.getSourceAccount() != null ? savedTransaction.getSourceAccount().getUserId() : null) // Użyj userId z konta źródłowego
                    .build();
            eventProducer.publishTransactionCompletedEvent(completedEvent); // Wysyłamy do Kafki
            log.info("TransactionCompletedEvent published for transaction ID: {}", savedTransaction.getId());

            // Audit log dla pomyślnej transakcji
            auditService.logEvent(
                    savedTransaction.getSourceAccount() != null ? String.valueOf(savedTransaction.getSourceAccount().getUserId()) : "SYSTEM", // Użytkownik, który zainicjował
                    "TRANSACTION_COMPLETED",
                    "Transaction",
                    savedTransaction.getId(),
                    "Transakcja " + savedTransaction.getTransactionRef() + " typu " + savedTransaction.getType() + " zakończona pomyślnie.",
                    AuditLog.AuditStatus.SUCCESS
            );

            return savedTransaction;

        } catch (Exception e) {
            log.error("Transaction failed for ref: {}. Error: {}", transaction.getTransactionRef(), e.getMessage(), e);

            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setDescription("Transakcja nieudana: " + e.getMessage());
            // Zapisz transakcję ze statusem FAILED
            Transaction failedTransaction = transactionRepository.save(transaction);

            // Publikacja zdarzenia TransactionFailedEvent do KAFKI
            TransactionFailedEvent failedEvent = TransactionFailedEvent.builder()
                    .transactionId(failedTransaction.getId())
                    .sourceAccountId(failedTransaction.getSourceAccount() != null ? failedTransaction.getSourceAccount().getId() : null)
                    .targetAccountId(failedTransaction.getTargetAccount() != null ? failedTransaction.getTargetAccount().getId() : null)
                    .amount(failedTransaction.getAmount())
                    .currency(failedTransaction.getCurrency())
                    .transactionType(failedTransaction.getType() != null ? failedTransaction.getType().name() : null)
                    .reason(e.getMessage()) // Użyj komunikatu błędu jako powodu
                    .failedAt(failedTransaction.getTransactionDate())
                    .details(e.toString()) // Pełniejsze szczegóły wyjątku
                    .userId(failedTransaction.getSourceAccount() != null ? failedTransaction.getSourceAccount().getUserId() : null)
                    .build();
            eventProducer.publishTransactionFailedEvent(failedEvent); // Wysyłamy do Kafki
            log.warn("TransactionFailedEvent published for transaction ID: {}", failedEvent.getTransactionId());

            // Audit log dla nieudanej transakcji
            auditService.logEvent(
                    failedTransaction.getSourceAccount() != null ? String.valueOf(failedTransaction.getSourceAccount().getUserId()) : "SYSTEM",
                    "TRANSACTION_FAILED",
                    "Transaction",
                    failedTransaction.getId(),
                    "Transakcja " + failedTransaction.getTransactionRef() + " typu " + failedTransaction.getType() + " nieudana: " + e.getMessage(),
                    AuditLog.AuditStatus.FAILURE
            );

            throw e; // Rzuć ponownie wyjątek po odnotowaniu błędu
        }
    }

    private void handleTransfer(BankAccount sourceAccount, BankAccount targetAccount, BigDecimal amount) {
        if (sourceAccount == null || targetAccount == null) {
            throw new IllegalArgumentException("Dla przelewu wymagane są oba konta: źródłowe i docelowe.");
        }
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Niewystarczające środki na koncie źródłowym.");
        }

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        targetAccount.setBalance(targetAccount.getBalance().add(amount));

        bankAccountRepository.save(sourceAccount);
        bankAccountRepository.save(targetAccount);
    }

    private void handleDeposit(BankAccount targetAccount, BigDecimal amount) {
        if (targetAccount == null) {
            throw new IllegalArgumentException("Dla wpłaty wymagane jest konto docelowe.");
        }
        targetAccount.setBalance(targetAccount.getBalance().add(amount));
        bankAccountRepository.save(targetAccount);
    }

    private void handleWithdrawal(BankAccount sourceAccount, BigDecimal amount) {
        if (sourceAccount == null) {
            throw new IllegalArgumentException("Dla wypłaty wymagane jest konto źródłowe.");
        }
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Niewystarczające środki na koncie źródłowym.");
        }
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        bankAccountRepository.save(sourceAccount);
    }

    private void handleLoanRepayment(BankAccount sourceAccount, BigDecimal amount) {
        if (sourceAccount == null) {
            throw new IllegalArgumentException("Dla spłaty pożyczki wymagane jest konto źródłowe.");
        }
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Niewystarczające środki na koncie do spłaty pożyczki.");
        }
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        bankAccountRepository.save(sourceAccount);
    }

    private void handleInterestPayout(BankAccount targetAccount, BigDecimal amount) {
        if (targetAccount == null) {
            throw new IllegalArgumentException("Dla wypłaty odsetek wymagane jest konto docelowe.");
        }
        targetAccount.setBalance(targetAccount.getBalance().add(amount));
        bankAccountRepository.save(targetAccount);
    }

    private void handleFee(BankAccount sourceAccount, BigDecimal amount) {
        if (sourceAccount == null) {
            throw new IllegalArgumentException("Dla opłaty wymagane jest konto źródłowe.");
        }
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Niewystarczające środki na koncie na pokrycie opłaty.");
        }
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        bankAccountRepository.save(sourceAccount);
    }


    @Transactional(readOnly = true)
    public Optional<Transaction> getTransactionById(Long id) {
        return transactionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Transaction> getTransactionByRef(String transactionRef) {
        return transactionRepository.findByTransactionRef(transactionRef);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByAccountId(Long accountId) {
        return transactionRepository.findBySourceAccountIdOrTargetAccountIdOrderByTransactionDateDesc(accountId, accountId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByAccountIdAndDateRange(Long accountId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Transaction> sourceTransactions = transactionRepository.findBySourceAccountIdAndTransactionDateBetween(accountId, startDate, endDate);
        List<Transaction> targetTransactions = transactionRepository.findByTargetAccountIdAndTransactionDateBetween(accountId, startDate, endDate);
        sourceTransactions.addAll(targetTransactions);
        return sourceTransactions.stream().distinct().toList();
    }

    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Tworzy transakcję wpłaty i przekazuje ją do głównej metody processTransaction.
     *
     * @param targetAccountId ID konta docelowego.
     * @param amount Kwota wpłaty.
     * @param currency Waluta wpłaty.
     * @param description Opis transakcji.
     * @param transactionDateTime Data i czas transakcji.
     * @return Zapisana transakcja.
     */
    @Transactional
    public Transaction createDepositTransaction(Long targetAccountId, BigDecimal amount, String currency, String description, LocalDateTime transactionDateTime) {
        log.info("Recording deposit of {} {} to account ID {}", amount, currency, targetAccountId);

        BankAccount targetAccount = bankAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Target account with ID " + targetAccountId + " not found for deposit."));

        if (!targetAccount.getCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Currency mismatch for deposit. Expected: " + targetAccount.getCurrency() + ", Got: " + currency);
        }

        Transaction newTransaction = Transaction.builder()
                .targetAccount(targetAccount)
                .amount(amount)
                .currency(currency)
                .description(description)
                .type(Transaction.TransactionType.DEPOSIT)
                .transactionDate(transactionDateTime)
                .build();

        return processTransaction(newTransaction);
    }

    /**
     * Obsługuje wpłatę środków na konto.
     * Ta metoda została zrefaktoryzowana, aby wykorzystywać ujednoliconą metodę processTransaction.
     *
     * @param accountId ID konta.
     * @param amount Kwota do wpłaty.
     * @param username Nazwa użytkownika inicjującego operację.
     * @return Zaktualizowane konto bankowe.
     */
    @Transactional
    public BankAccount depositFunds(Long accountId, BigDecimal amount, String username) {
        log.info("Attempting to deposit {} to account {}", amount, accountId);
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        // Tworzymy obiekt Transaction dla wpłaty
        Transaction depositTransaction = Transaction.builder()
                .targetAccount(account)
                .amount(amount)
                .currency(account.getCurrency()) // Zakładamy, że waluta jest walutą konta
                .type(Transaction.TransactionType.DEPOSIT)
                .description("Deposit by " + username)
                .transactionDate(LocalDateTime.now())
                .build();

        processTransaction(depositTransaction); // Przetwarzamy transakcję przez główną metodę

        // Po pomyślnym przetworzeniu transakcji, pobieramy zaktualizowane konto
        // (stan konta został zmieniony w processTransaction)
        return bankAccountRepository.findById(accountId).orElseThrow(() ->
                new IllegalStateException("Account disappeared after deposit transaction!"));
    }

    /**
     * Obsługuje wypłatę środków z konta.
     * Ta metoda została zrefaktoryzowana, aby wykorzystywać ujednoliconą metodę processTransaction.
     *
     * @param accountId ID konta.
     * @param amount Kwota do wypłaty.
     * @param username Nazwa użytkownika inicjującego operację.
     * @return Zaktualizowane konto bankowe.
     * @throws InsufficientFundsException jeśli konto ma niewystarczające środki.
     */
    @Transactional
    public BankAccount withdrawFunds(Long accountId, BigDecimal amount, String username) {
        log.info("Attempting to withdraw {} from account {}", amount, accountId);
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        // Tworzymy obiekt Transaction dla wypłaty
        Transaction withdrawalTransaction = Transaction.builder()
                .sourceAccount(account)
                .amount(amount)
                .currency(account.getCurrency()) // Zakładamy, że waluta jest walutą konta
                .type(Transaction.TransactionType.WITHDRAWAL)
                .description("Withdrawal by " + username)
                .transactionDate(LocalDateTime.now())
                .build();

        processTransaction(withdrawalTransaction); // Przetwarzamy transakcję przez główną metodę

        // Po pomyślnym przetworzeniu transakcji, pobieramy zaktualizowane konto
        return bankAccountRepository.findById(accountId).orElseThrow(() ->
                new IllegalStateException("Account disappeared after withdrawal transaction!"));
    }

    /**
     * Tworzy transakcję wypłaty odsetek i przekazuje ją do głównej metody processTransaction.
     *
     * @param targetAccount Konto docelowe dla odsetek.
     * @param amount Kwota odsetek.
     * @return Zapisana transakcja.
     */
    @Transactional
    public Transaction createInterestPayoutTransaction(BankAccount targetAccount, BigDecimal amount) {
        log.info("Recording interest payout of {} {} to account ID {}", amount, targetAccount.getCurrency(), targetAccount.getId());

        Transaction newTransaction = Transaction.builder()
                .targetAccount(targetAccount)
                .amount(amount)
                .currency(targetAccount.getCurrency())
                .type(Transaction.TransactionType.INTEREST_PAYOUT)
                .description("Daily interest payout")
                .transactionDate(LocalDateTime.now())
                .build();
        return processTransaction(newTransaction);
    }

    /**
     * Tworzy transakcję przelewu i przekazuje ją do głównej metody processTransaction.
     *
     * @param sourceAccountId ID konta źródłowego.
     * @param targetAccountNumber Numer konta docelowego.
     * @param amount Kwota przelewu.
     * @param currency Waluta transakcji.
     * @param description Opis transakcji.
     * @param transactionDateTime Data i czas transakcji.
     * @return Zapisana transakcja.
     */
    @Transactional
    public Transaction createTransferTransaction(Long sourceAccountId, String targetAccountNumber,
                                                 BigDecimal amount, String currency, String description,
                                                 LocalDateTime transactionDateTime) {
        log.info("Initiating transfer from account ID {} to account number {} for amount {} {}",
                sourceAccountId, targetAccountNumber, amount, currency);

        BankAccount sourceAccount = bankAccountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Source account with ID " + sourceAccountId + " not found."));

        BankAccount targetAccount = bankAccountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Target account with number " + targetAccountNumber + " not found."));

        if (!sourceAccount.getCurrency().equalsIgnoreCase(currency) || !targetAccount.getCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Currency mismatch for transfer transaction. Source: " + sourceAccount.getCurrency() + ", Target: " + targetAccount.getCurrency() + ", Transaction: " + currency);
        }

        Transaction newTransaction = Transaction.builder()
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(amount)
                .currency(currency)
                .description(description)
                .type(Transaction.TransactionType.TRANSFER)
                .transactionDate(transactionDateTime)
                .build();

        return processTransaction(newTransaction);
    }

    // USUNIĘTA METODA: processTransfer(TransactionRequest request)
    // Ta metoda była duplikatem i została usunięta na rzecz ujednoliconej metody processTransaction
    // oraz metod create...Transaction, które budują obiekt Transaction i przekazują go do processTransaction.
}
