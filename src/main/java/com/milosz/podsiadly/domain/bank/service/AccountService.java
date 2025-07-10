package com.milosz.podsiadly.domain.bank.service;
import com.milosz.podsiadly.common.exception.ResourceNotFoundException;
import com.milosz.podsiadly.domain.bank.dto.AccountDto;
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
    // --- RENAMED AND MODIFIED METHOD ---
    @Transactional
    public BankAccount createBankAccount(Long userId, Long bankId, String accountType, String currency, String username) {
        log.info("Attempting to create a new {} account for user {} at bank {}", accountType, userId, bankId);

        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Fetch the Bank entity
        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank not found with id: " + bankId));

        BankAccount newAccount = BankAccount.builder()
                .accountNumber(generateUniqueAccountNumber()) // Use a dedicated method for this
                .accountType(accountType) // Set the account type
                .balance(BigDecimal.ZERO) // Initial balance is typically zero on creation
                .currency(currency)
                .status(BankAccount.AccountStatus.ACTIVE)
                // createdAt and updatedAt are handled by @PrePersist
                .userId(userId)
                .bank(bank) // Set the associated Bank
                .build();

        BankAccount savedAccount = bankAccountRepository.save(newAccount);

        auditService.logEvent(
                username,
                "ACCOUNT_CREATED",
                "BankAccount",
                savedAccount.getId(),
                "New account created for user " + userId + ". Type: " + accountType + ", Currency: " + currency + ", Bank: " + bank.getName(),
                AuditLog.AuditStatus.SUCCESS
        );
        log.info("Account {} created successfully for user {}.", savedAccount.getAccountNumber(), userId);
        return savedAccount;
    }

    // New helper method for account number generation
    private String generateUniqueAccountNumber() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
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
    public BankAccount updateAccount(Long id, AccountDto accountDto, String username) { // Added username for auditing
        log.info("User {} attempting to update account ID {}", username, id);

        BankAccount existingAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        // Apply updates from DTO to the existing entity
        // Only update fields that are meant to be changeable via PUT
        if (accountDto.balance() != null) {
            existingAccount.setBalance(accountDto.balance());
        }
        if (accountDto.currency() != null && !accountDto.currency().isBlank()) {
            existingAccount.setCurrency(accountDto.currency());
        }
        if (accountDto.status() != null) {
            existingAccount.setStatus(accountDto.status());
        }
        // accountNumber, userId, bank, createdAt are typically NOT updated via this method.
        // If accountType is also updatable, add: existingAccount.setAccountType(accountDto.getAccountType());

        existingAccount.setUpdatedAt(LocalDateTime.now()); // Update timestamp

        BankAccount updatedAccount = bankAccountRepository.save(existingAccount);

        auditService.logEvent(
                username,
                "ACCOUNT_UPDATED",
                "BankAccount",
                id,
                "Account " + updatedAccount.getAccountNumber() + " updated. New status: " + updatedAccount.getStatus(),
                AuditLog.AuditStatus.SUCCESS
        );
        log.info("Account {} updated successfully by user {}.", updatedAccount.getAccountNumber(), username);
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
