package com.milosz.podsiadly.domain.bank.repository;


import com.milosz.podsiadly.domain.bank.model.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, Long> {
    Optional<Deposit> findByDepositNumber(String depositNumber);
    List<Deposit> findByAccountId(Long accountId);
    List<Deposit> findByStatus(Deposit.DepositStatus status);
}
