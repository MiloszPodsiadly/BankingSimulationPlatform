package com.milosz.podsiadly.domain.bank.service;

import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.repository.BankAccountRepository;
import com.milosz.podsiadly.domain.bank.repository.TransactionRepository;
import com.milosz.podsiadly.core.event.TransactionCompletedEvent;
import com.milosz.podsiadly.core.event.TransactionFailedEvent;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransactionService(TransactionRepository transactionRepository, BankAccountRepository bankAccountRepository, ApplicationEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Transaction processTransaction(Transaction transaction) {
        BankAccount sourceAccount = transaction.getSourceAccount();
        BankAccount targetAccount = transaction.getTargetAccount();
        BigDecimal amount = transaction.getAmount();

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

        transaction.setTransactionRef("TRN-" + UUID.randomUUID().toString());
        transaction.setStatus(Transaction.TransactionStatus.PENDING); // Początkowy status

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
                    // Logika spłaty pożyczki jest głównie w LoanService, tu tylko odnotowujemy transakcję
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
            transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
            eventPublisher.publishEvent(new TransactionCompletedEvent(this, transaction.getId(), transaction.getTransactionRef()));

        } catch (Exception e) {
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setDescription("Transakcja nieudana: " + e.getMessage());
            eventPublisher.publishEvent(new TransactionFailedEvent(this, transaction.getId(), transaction.getTransactionRef(), e.getMessage()));
            throw e; // Rzuć ponownie wyjątek po odnotowaniu błędu
        } finally {
            return transactionRepository.save(transaction);
        }
    }

    private void handleTransfer(BankAccount sourceAccount, BankAccount targetAccount, BigDecimal amount) {
        if (sourceAccount == null || targetAccount == null) {
            throw new IllegalArgumentException("Dla przelewu wymagane są oba konta: źródłowe i docelowe.");
        }
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Niewystarczające środki na koncie źródłowym.");
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
            throw new IllegalStateException("Niewystarczające środki na koncie źródłowym.");
        }
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        bankAccountRepository.save(sourceAccount);
    }

    private void handleLoanRepayment(BankAccount sourceAccount, BigDecimal amount) {
        if (sourceAccount == null) {
            throw new IllegalArgumentException("Dla spłaty pożyczki wymagane jest konto źródłowe.");
        }
        // Logika faktycznej spłaty i aktualizacji pożyczki jest w LoanService
        // Tutaj tylko upewniamy się, że konto ma środki i odejmujemy je.
        if (sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Niewystarczające środki na koncie do spłaty pożyczki.");
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
            throw new IllegalStateException("Niewystarczające środki na koncie na pokrycie opłaty.");
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
        // Może wymagać bardziej złożonego zapytania w repozytorium, aby uwzględnić oba konta
        List<Transaction> sourceTransactions = transactionRepository.findBySourceAccountIdAndTransactionDateBetween(accountId, startDate, endDate);
        List<Transaction> targetTransactions = transactionRepository.findByTargetAccountIdAndTransactionDateBetween(accountId, startDate, endDate);
        sourceTransactions.addAll(targetTransactions);
        return sourceTransactions.stream().distinct().toList(); // Usuń duplikaty, jeśli transakcja jest na to samo konto
    }

    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }
}
