package com.milosz.podsiadly.domain.bank.repository;

import com.milosz.podsiadly.domain.bank.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findByLoanNumber(String loanNumber);
    List<Loan> findByAccountId(Long accountId);
    List<Loan> findByStatus(Loan.LoanStatus status);
}
