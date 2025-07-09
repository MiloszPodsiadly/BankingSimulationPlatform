package com.milosz.podsiadly.domain.bank.service;

import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.model.Deposit;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.repository.BankAccountRepository;
import com.milosz.podsiadly.domain.bank.repository.DepositRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DepositService {

    private final DepositRepository depositRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionService transactionService; // Do tworzenia transakcji depozytu

    public DepositService(DepositRepository depositRepository, BankAccountRepository bankAccountRepository, TransactionService transactionService) {
        this.depositRepository = depositRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public Deposit createDeposit(Deposit deposit, Long accountId) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Bank account with ID " + accountId + " not found."));

        if (deposit.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("The deposit amount must be positive.");
        }
        if (deposit.getTermMonths() <= 0) {
            throw new IllegalArgumentException("The deposit period must be positive.");
        }

        deposit.setAccount(account);
        deposit.setDepositNumber("DEP-" + UUID.randomUUID().toString());
        deposit.setStartDate(LocalDate.now());
        deposit.setEndDate(deposit.getStartDate().plusMonths(deposit.getTermMonths()));
        deposit.setStatus(Deposit.DepositStatus.ACTIVE);
        //deposit.setOutstandingBalance(deposit.getAmount()); // Na początku, outstandingBalance to pełna kwota

        // Zablokuj środki na koncie bankowym
        account.setBalance(account.getBalance().subtract(deposit.getAmount()));
        bankAccountRepository.save(account);

        // Utwórz transakcję dla depozytu
        Transaction depositTransaction = new Transaction();
        depositTransaction.setSourceAccount(account); // Konto źródłowe dla depozytu jest jednocześnie docelowym
        depositTransaction.setTargetAccount(account);
        depositTransaction.setAmount(deposit.getAmount());
        depositTransaction.setCurrency(account.getCurrency());
        depositTransaction.setType(Transaction.TransactionType.DEPOSIT);
        depositTransaction.setDescription("Creating a deposit " + deposit.getDepositNumber());
        transactionService.processTransaction(depositTransaction); // Użyj TransactionService do przetworzenia

        return depositRepository.save(deposit);
    }

    @Transactional(readOnly = true)
    public Optional<Deposit> getDepositById(Long id) {
        return depositRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Deposit> getDepositsByAccountId(Long accountId) {
        return depositRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<Deposit> getAllDeposits() {
        return depositRepository.findAll();
    }

    @Transactional
    public Deposit updateDepositStatus(Long id, Deposit.DepositStatus newStatus) {
        Deposit existingDeposit = depositRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Deposit with ID " + id + " not found."));
        existingDeposit.setStatus(newStatus);
        return depositRepository.save(existingDeposit);
    }

    @Transactional
    public void deleteDeposit(Long id) {
        if (!depositRepository.existsById(id)) {
            throw new EntityNotFoundException("Deposit with ID " + id + " not found.");
        }
        // Rozważ logikę zwrotu środków na konto, jeśli lokata jest aktywna
        depositRepository.deleteById(id);
    }

    /**
     * Procesuje lokatę po jej dojrzewaniu.
     * Zwraca kwotę główną + odsetki na konto użytkownika.
     */
    @Transactional
    public void matureDeposit(Long depositId) {
        Deposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new EntityNotFoundException("Deposit with ID " + depositId + " not found."));

        if (deposit.getStatus() != Deposit.DepositStatus.ACTIVE || !deposit.getEndDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("The deposit is not active or has not reached maturity.");
        }

        BigDecimal totalPayout = deposit.getAmount().add(
                deposit.getAmount().multiply(deposit.getInterestRate())
                        .multiply(BigDecimal.valueOf(deposit.getTermMonths()))
                        .divide(BigDecimal.valueOf(12), BigDecimal.ROUND_HALF_UP)
        );

        BankAccount account = deposit.getAccount();
        account.setBalance(account.getBalance().add(totalPayout));
        bankAccountRepository.save(account);

        deposit.setStatus(Deposit.DepositStatus.MATURED);
        depositRepository.save(deposit);

        // Utwórz transakcję dla wypłaty lokaty z odsetkami
        Transaction payoutTransaction = new Transaction();
        payoutTransaction.setSourceAccount(account); // Wypłata z lokaty na to samo konto
        payoutTransaction.setTargetAccount(account);
        payoutTransaction.setAmount(totalPayout);
        payoutTransaction.setCurrency(account.getCurrency());
        payoutTransaction.setType(Transaction.TransactionType.INTEREST_PAYOUT); // lub WITHDRAWAL
        payoutTransaction.setDescription("Withdrawal of funds from the deposit " + deposit.getDepositNumber() + " along with interest.");
        transactionService.processTransaction(payoutTransaction);
    }
}
