// domain/report/repository/BankStatementRepository.java
package com.milosz.podsiadly.domain.report.repository;

import com.milosz.podsiadly.domain.report.model.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {
    // You might add custom queries here, e.g., find by accountId, by date range
}