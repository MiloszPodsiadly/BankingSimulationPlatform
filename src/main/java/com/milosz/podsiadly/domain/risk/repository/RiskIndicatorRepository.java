package com.milosz.podsiadly.domain.risk.repository;

import com.milosz.podsiadly.domain.risk.model.RiskIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RiskIndicatorRepository extends JpaRepository<RiskIndicator, Long> {
    Optional<RiskIndicator> findByIndicatorCode(String indicatorCode);
    boolean existsByIndicatorCode(String indicatorCode);
}