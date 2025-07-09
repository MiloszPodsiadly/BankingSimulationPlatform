package com.milosz.podsiadly.domain.bank.service;

import com.milosz.podsiadly.domain.bank.model.Bank;
import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.repository.BankAccountRepository;
import com.milosz.podsiadly.domain.bank.repository.BankRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    private final BankAccountRepository bankAccountRepository;
    private final BankRepository bankRepository; // Potrzebne do powiązania konta z bankiem

    public AccountService(BankAccountRepository bankAccountRepository, BankRepository bankRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankRepository = bankRepository;
    }

    @Transactional
    public BankAccount createAccount(BankAccount account, Long bankId) {
        if (bankAccountRepository.findByAccountNumber(account.getAccountNumber()).isPresent()) {
            throw new IllegalArgumentException("An account with the given number already exists.");
        }
        Bank bank = bankRepository.findById(bankId)
                .orElseThrow(() -> new EntityNotFoundException("Bank with ID " + bankId + " not found."));
        account.setBank(bank);
        account.setStatus(BankAccount.AccountStatus.ACTIVE); // Domyślny status
        return bankAccountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Optional<BankAccount> getAccountById(Long id) {
        return bankAccountRepository.findById(id);
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
    public BankAccount updateAccount(Long id, BankAccount updatedAccount) {
        BankAccount existingAccount = bankAccountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bank account with ID " + id + " not found."));

        existingAccount.setBalance(updatedAccount.getBalance());
        existingAccount.setCurrency(updatedAccount.getCurrency());
        existingAccount.setStatus(updatedAccount.getStatus());
        // Zakładamy, że bank i userId nie mogą być zmieniane po utworzeniu konta
        // existingAccount.setUserId(updatedAccount.getUserId());
        // existingAccount.setBank(updatedAccount.getBank());

        return bankAccountRepository.save(existingAccount);
    }

    @Transactional
    public void deleteAccount(Long id) {
        if (!bankAccountRepository.existsById(id)) {
            throw new EntityNotFoundException("Bank account with ID " + id + " not found.");
        }
        bankAccountRepository.deleteById(id);
    }

    @Transactional
    public BankAccount updateAccountBalance(Long accountId, BigDecimal amount) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Bank account with ID " + accountId + " not found."));
        account.setBalance(account.getBalance().add(amount));
        return bankAccountRepository.save(account);
    }
}
