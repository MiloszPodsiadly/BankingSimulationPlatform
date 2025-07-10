package com.milosz.podsiadly.domain.bank.service;

import com.milosz.podsiadly.common.exception.InsufficientFundsException;
import com.milosz.podsiadly.common.exception.ResourceNotFoundException;
import com.milosz.podsiadly.domain.bank.dto.TransactionRequest;
import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.repository.BankAccountRepository;
import com.milosz.podsiadly.domain.bank.repository.TransactionRepository;
import com.milosz.podsiadly.core.event.TransactionCompletedEvent;
import com.milosz.podsiadly.core.event.TransactionFailedEvent;
import com.milosz.podsiadly.domain.compliance.model.AuditLog;
import com.milosz.podsiadly.domain.compliance.service.AuditService;
import com.milosz.podsiadly.domain.bank.mapper.TransactionMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository bankAccountRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final TransactionMapper transactionMapper;




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
    @Transactional
    public Transaction createDepositTransaction(Long targetAccountId, BigDecimal amount, String currency, String description, LocalDateTime transactionDateTime) {
        log.info("Recording deposit of {} {} to account ID {}", amount, currency, targetAccountId);

        // Fetch the target bank account
        BankAccount targetAccount = bankAccountRepository.findById(targetAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Target account with ID " + targetAccountId + " not found for deposit."));

        // Basic validation (e.g., currency match)
        if (!targetAccount.getCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Currency mismatch for deposit. Expected: " + targetAccount.getCurrency() + ", Got: " + currency);
        }

        // Update the account balance
        targetAccount.setBalance(targetAccount.getBalance().add(amount));
        bankAccountRepository.save(targetAccount); // Save the updated account balance

        // Create the transaction record
        Transaction transaction = Transaction.builder()
                .sourceAccount(null) // For a deposit, the source is external to the system, so 'null' or a special system account
                .targetAccount(targetAccount)
                .amount(amount)
                .currency(currency)
                .description(description)
                .type(Transaction.TransactionType.DEPOSIT) // Make sure this enum value exists in Transaction.java
                .status(Transaction.TransactionStatus.COMPLETED) // Correct // Make sure this enum value exists
                .transactionDate(transactionDateTime)
                .build();

        // Save the transaction record
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Deposit transaction recorded: {}", savedTransaction.getId());
        return savedTransaction;
    }
    @Transactional
    public BankAccount depositFunds(Long accountId, BigDecimal amount, String username) {
        log.info("Attempting to deposit {} to account {}", amount, accountId);
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        account.setBalance(account.getBalance().add(amount));
        BankAccount updatedAccount = bankAccountRepository.save(account);

        // Log successful deposit
        auditService.logEvent(
                username,
                "DEPOSIT_FUNDS",
                "BankAccount",
                accountId,
                "Deposited " + amount + " to account " + accountId + ". New balance: " + updatedAccount.getBalance(),
                AuditLog.AuditStatus.SUCCESS
        );
        log.info("Deposit of {} to account {} successful.", amount, accountId);
        return updatedAccount;
    }
    // Example: Withdraw funds
    @Transactional
    public BankAccount withdrawFunds(Long accountId, BigDecimal amount, String username) {
        log.info("Attempting to withdraw {} from account {}", amount, accountId);
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        if (account.getBalance().compareTo(amount) < 0) {
            // Log failure
            auditService.logEvent(
                    username,
                    "WITHDRAWAL_FAILED_INSUFFICIENT_FUNDS",
                    "BankAccount",
                    accountId,
                    "Attempted withdrawal of " + amount + " from account " + accountId + ". Insufficient funds. Balance: " + account.getBalance(),
                    AuditLog.AuditStatus.FAILURE
            );
            throw new InsufficientFundsException("Account " + accountId + " has insufficient funds for withdrawal.");
        }

        account.setBalance(account.getBalance().subtract(amount));
        BankAccount updatedAccount = bankAccountRepository.save(account);

        // Log successful withdrawal
        auditService.logEvent(
                username,
                "WITHDRAW_FUNDS",
                "BankAccount",
                accountId,
                "Withdrew " + amount + " from account " + accountId + ". New balance: " + updatedAccount.getBalance(),
                AuditLog.AuditStatus.SUCCESS
        );
        log.info("Withdrawal of {} from account {} successful.", amount, accountId);
        return updatedAccount;
    }
    @Transactional
    public Transaction createInterestPayoutTransaction(BankAccount targetAccount, BigDecimal amount) {
        // Tworzy transakcję typu INTEREST_PAYOUT
        Transaction transaction = Transaction.builder()
                .transactionRef("INT-" + UUID.randomUUID().toString())
                .targetAccount(targetAccount) // Odsetki wpływają na konto
                .amount(amount)
                .currency(targetAccount.getCurrency())
                .type(Transaction.TransactionType.INTEREST_PAYOUT) // Upewnij się, że masz ten typ w Transaction.TransactionType
                .status(Transaction.TransactionStatus.COMPLETED)
                .description("Daily interest payout")
                .transactionDate(LocalDateTime.now())
                .build();
        return transactionRepository.save(transaction);
    }
    @Transactional
    public Transaction createTransferTransaction(Long sourceAccountId, String targetAccountNumber,
                                                 BigDecimal amount, String currency, String description,
                                                 LocalDateTime transactionDateTime) {
        log.info("Initiating transfer from account ID {} to account number {} for amount {} {}",
                sourceAccountId, targetAccountNumber, amount, currency);

        BankAccount sourceAccount = bankAccountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Source account with ID " + sourceAccountId + " not found."));

        BankAccount targetAccount = bankAccountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new EntityNotFoundException("Target account with number " + targetAccountNumber + " not found."));

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
    @Transactional
    public Transaction processTransfer(TransactionRequest request) {
        // 1. Zmapuj TransactionRequest do encji Transaction (zignorowane pola są puste)
        Transaction transaction = transactionMapper.toEntity(request);

        // 2. Pobierz konta bankowe na podstawie ID/numerów z requestu
        BankAccount sourceAccount = bankAccountRepository.findById(request.sourceAccountId())
                .orElseThrow(() -> new RuntimeException("Source account not found"));
        // W przypadku targetAccountNumber, musisz znaleźć BankAccount po accountNumber
        BankAccount targetAccount = bankAccountRepository.findByAccountNumber(request.targetAccountNumber())
                .orElseThrow(() -> new RuntimeException("Target account not found"));

        // 3. Ustaw brakujące pola w encji Transaction
        transaction.setSourceAccount(sourceAccount);
        transaction.setTargetAccount(targetAccount);
        transaction.setType(Transaction.TransactionType.TRANSFER); // Ustaw typ transakcji
        transaction.setStatus(Transaction.TransactionStatus.PENDING); // Ustaw początkowy status
        // transaction.setTransactionDate() jest ustawiane w @PrePersist, ale możesz je ustawić tu, jeśli chcesz
        // transaction.setTransactionRef() również powinien być ustawiony tutaj (np. UUID.randomUUID().toString())

        // 4. Wykonaj logikę biznesową (walidacja, aktualizacja sald, itd.)
        // np. sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.amount()));
        //      targetAccount.setBalance(targetAccount.getBalance().add(request.amount()));
        // bankAccountRepository.save(sourceAccount);
        // bankAccountRepository.save(targetAccount);

        // 5. Zapisz transakcję
        return transactionRepository.save(transaction); // Załóżmy, że masz transactionRepository
    }
}
