package com.milosz.podsiadly.domain.bank.service;
import com.milosz.podsiadly.common.exception.ResourceNotFoundException;
import com.milosz.podsiadly.domain.user.repository.UserRepository;
import com.milosz.podsiadly.domain.bank.model.Bank;
import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.repository.BankAccountRepository;
import com.milosz.podsiadly.domain.bank.repository.BankRepository;
import com.milosz.podsiadly.domain.compliance.model.AuditLog;
import com.milosz.podsiadly.domain.compliance.service.AuditService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.apache.kafka.common.requests.DeleteAclsResponse.log;

@RequiredArgsConstructor
@Service
public class AccountService {

    private final BankAccountRepository bankAccountRepository;
    private final BankRepository bankRepository; // Potrzebne do powiązania konta z bankiem
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    @Transactional
    public BankAccount createAccount(Long userId, String accountType, String currency, String username) {
        log.info("Attempting to create a new {} account for user {}", accountType, userId);

        // Fetch user to link account (assuming user exists)
        // In a real app, you might validate user existence more thoroughly
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        BankAccount newAccount = BankAccount.builder()
                .accountNumber(UUID.randomUUID().toString()) // Generate unique account number
                .accountType(accountType)
                .balance(BigDecimal.ZERO)
                .currency(currency)
                .status(BankAccount.AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .userId(userId) // Link to the user
                .build();

        BankAccount savedAccount = bankAccountRepository.save(newAccount);

        // Log successful account creation
        auditService.logEvent(
                username,
                "ACCOUNT_CREATED",
                "BankAccount",
                savedAccount.getId(),
                "New account created for user " + userId + ". Type: " + accountType + ", Currency: " + currency,
                AuditLog.AuditStatus.SUCCESS
        );
        log.info("Account {} created successfully for user {}.", savedAccount.getAccountNumber(), userId);
        return savedAccount;
    }

    @Transactional(readOnly = true)
    public BankAccount getAccountById(Long accountId, String username) {
        log.info("User {} attempting to access account details for account id {}", username, accountId);
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        // Log successful account access (can be critical for compliance)
        auditService.logEvent(
                username,
                "ACCOUNT_ACCESSED",
                "BankAccount",
                accountId,
                "User accessed details for account " + account.getAccountNumber(),
                AuditLog.AuditStatus.SUCCESS
        );
        return account;
    }

    @Transactional(readOnly = true)
    public Optional<BankAccount> getAccountByAccountNumber(String accountNumber) {
        return bankAccountRepository.findByAccountNumber(accountNumber);
    }

    @Transactional(readOnly = true)
    public List<BankAccount> getAccountsByUserId(Long userId) {
        return bankAccountRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }

    @Transactional
    public BankAccount updateAccountStatus(Long accountId, BankAccount.AccountStatus newStatus, String username) {
        log.info("User {} attempting to update status of account {} to {}", username, accountId, newStatus);
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        BankAccount.AccountStatus oldStatus = account.getStatus();
        account.setStatus(newStatus);
        BankAccount updatedAccount = bankAccountRepository.save(account);

        // Log account status update
        auditService.logEvent(
                username,
                "ACCOUNT_STATUS_UPDATED",
                "BankAccount",
                accountId,
                "Account " + account.getAccountNumber() + " status changed from " + oldStatus + " to " + newStatus,
                AuditLog.AuditStatus.SUCCESS
        );
        log.info("Account {} status updated to {}", accountId, newStatus);
        return updatedAccount;
    }
        // Zakładamy, że bank i userId nie mogą być zmieniane po utworzeniu konta
        // existingAccount.setUserId(updatedAccount.getUserId());
        // existingAccount.setBank(updatedAccount.getBank());


    // Example: Delete account (sensitive operation)
    @Transactional
    public void deleteAccount(Long accountId, String username) {
        log.info("User {} attempting to delete account {}", username, accountId);
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        bankAccountRepository.delete(account);

        // Log account deletion
        auditService.logEvent(
                username,
                "ACCOUNT_DELETED",
                "BankAccount",
                accountId,
                "Account " + account.getAccountNumber() + " deleted.",
                AuditLog.AuditStatus.SUCCESS
        );
        log.info("Account {} deleted successfully.", accountId);
    }

    @Transactional
    public BankAccount updateAccountBalance(Long accountId, BigDecimal amount) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Bank account with ID " + accountId + " not found."));
        account.setBalance(account.getBalance().add(amount));
        return bankAccountRepository.save(account);
    }
    @Transactional
    public void applyDailyInterest() {
        log.info("Applying daily interest to all active accounts.");
        List<BankAccount> activeAccounts = bankAccountRepository.findByStatus(BankAccount.AccountStatus.ACTIVE);

        for (BankAccount account : activeAccounts) {
            // Prosta logika naliczania odsetek: np. 0.01% dziennie
            BigDecimal dailyInterestRate = new BigDecimal("0.0001"); // 0.01%
            BigDecimal interestAmount = account.getBalance().multiply(dailyInterestRate)
                    .setScale(2, RoundingMode.HALF_UP);

            if (interestAmount.compareTo(BigDecimal.ZERO) > 0) {
                account.setBalance(account.getBalance().add(interestAmount));
                bankAccountRepository.save(account);

                // Opcjonalnie: Zapisz to jako transakcję typu INTEREST_PAYOUT
                // Upewnij się, że masz TransactionType.INTEREST_PAYOUT w Transaction.java
                transactionService.createInterestPayoutTransaction(account, interestAmount); // Zakładam taką metodę w TransactionService
                log.debug("Applied {} interest to account {}", interestAmount, account.getAccountNumber());
            }
        }
        log.info("Daily interest application completed.");
    }
}
