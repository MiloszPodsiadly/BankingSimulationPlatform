package com.milosz.podsiadly.domain.bank.repository;


import com.milosz.podsiadly.domain.bank.model.Bank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {
    Optional<Bank> findByName(String name);
    Optional<Bank> findByBic(String bic);
}
