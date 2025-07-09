package com.milosz.podsiadly.domain.bank.service;

import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.model.Loan;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.repository.BankAccountRepository;
import com.milosz.podsiadly.domain.bank.repository.LoanRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionService transactionService; // Do tworzenia transakcji kredytowych

    public LoanService(LoanRepository loanRepository, BankAccountRepository bankAccountRepository, TransactionService transactionService) {
        this.loanRepository = loanRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public Loan createLoan(Loan loan, Long accountId) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Konto bankowe o ID " + accountId + " nie znaleziono."));

        if (loan.getPrincipalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota główna pożyczki musi być dodatnia.");
        }
        if (loan.getInterestRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Oprocentowanie musi być dodatnie.");
        }
        if (loan.getTermMonths() <= 0) {
            throw new IllegalArgumentException("Okres kredytowania musi być dodatni.");
        }

        loan.setAccount(account);
        loan.setLoanNumber("LOAN-" + UUID.randomUUID().toString());
        loan.setStartDate(LocalDate.now());
        loan.setEndDate(loan.getStartDate().plusMonths(loan.getTermMonths()));
        loan.setStatus(Loan.LoanStatus.ACTIVE);
        loan.setOutstandingBalance(loan.getPrincipalAmount()); // Na początku, zaległość to pełna kwota

        // Zasil konto bankowe kwotą pożyczki
        account.setBalance(account.getBalance().add(loan.getPrincipalAmount()));
        bankAccountRepository.save(account);

        // Utwórz transakcję dla wypłaty pożyczki
        Transaction loanPayoutTransaction = new Transaction();
        loanPayoutTransaction.setSourceAccount(account); // Zakładamy, że to konto banku (nie jest to do końca zgodne z rzeczywistością, ale upraszcza model)
        loanPayoutTransaction.setTargetAccount(account);
        loanPayoutTransaction.setAmount(loan.getPrincipalAmount());
        loanPayoutTransaction.setCurrency(account.getCurrency());
        loanPayoutTransaction.setType(Transaction.TransactionType.DEPOSIT); // Pożyczka jest "depozytem" na konto klienta
        loanPayoutTransaction.setDescription("Wypłata pożyczki " + loan.getLoanNumber() + " na konto.");
        transactionService.processTransaction(loanPayoutTransaction);

        return loanRepository.save(loan);
    }

    @Transactional(readOnly = true)
    public Optional<Loan> getLoanById(Long id) {
        return loanRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Loan> getLoansByAccountId(Long accountId) {
        return loanRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    @Transactional
    public Loan updateLoanStatus(Long id, Loan.LoanStatus newStatus) {
        Loan existingLoan = loanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pożyczka o ID " + id + " nie znaleziono."));
        existingLoan.setStatus(newStatus);
        return loanRepository.save(existingLoan);
    }

    @Transactional
    public void deleteLoan(Long id) {
        if (!loanRepository.existsById(id)) {
            throw new EntityNotFoundException("Pożyczka o ID " + id + " nie znaleziono.");
        }
        // Rozważ logikę, jeśli pożyczka jest aktywna
        loanRepository.deleteById(id);
    }

    @Transactional
    public Loan makeLoanRepayment(Long loanId, BigDecimal repaymentAmount) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Pożyczka o ID " + loanId + " nie znaleziono."));

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new IllegalStateException("Pożyczka nie jest aktywna.");
        }
        if (repaymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Kwota spłaty musi być dodatnia.");
        }

        BigDecimal newOutstandingBalance = loan.getOutstandingBalance().subtract(repaymentAmount);
        if (newOutstandingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Kwota spłaty przekracza pozostałą zaległość.");
        }

        loan.setOutstandingBalance(newOutstandingBalance);
        if (newOutstandingBalance.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(Loan.LoanStatus.PAID_OFF);
        }

        // Pobierz konto bankowe, z którego ma nastąpić spłata
        BankAccount account = loan.getAccount();
        if (account.getBalance().compareTo(repaymentAmount) < 0) {
            throw new IllegalStateException("Niewystarczające środki na koncie do spłaty pożyczki.");
        }
        account.setBalance(account.getBalance().subtract(repaymentAmount));
        bankAccountRepository.save(account);

        // Utwórz transakcję dla spłaty pożyczki
        Transaction repaymentTransaction = new Transaction();
        repaymentTransaction.setSourceAccount(account); // Konto klienta
        repaymentTransaction.setTargetAccount(account); // Konto banku (upraszczamy)
        repaymentTransaction.setAmount(repaymentAmount);
        repaymentTransaction.setCurrency(account.getCurrency());
        repaymentTransaction.setType(Transaction.TransactionType.LOAN_REPAYMENT);
        repaymentTransaction.setDescription("Spłata pożyczki " + loan.getLoanNumber());
        transactionService.processTransaction(repaymentTransaction);

        return loanRepository.save(loan);
    }
}
