package com.milosz.podsiadly.domain.bank.repository;

import com.milosz.podsiadly.domain.bank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findByTransactionRef(String transactionRef);
    List<Transaction> findBySourceAccountIdOrTargetAccountIdOrderByTransactionDateDesc(Long sourceAccountId, Long targetAccountId);
    List<Transaction> findBySourceAccountIdAndTransactionDateBetween(Long sourceAccountId, LocalDateTime startDate, LocalDateTime endDate);
    List<Transaction> findByTargetAccountIdAndTransactionDateBetween(Long targetAccountId, LocalDateTime startDate, LocalDateTime endDate);
}