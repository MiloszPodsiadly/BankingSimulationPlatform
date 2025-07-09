package com.milosz.podsiadly.domain.risk.service;

import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.repository.BankAccountRepository;
import com.milosz.podsiadly.domain.bank.repository.TransactionRepository;
import com.milosz.podsiadly.domain.risk.model.RiskAssessment;
import com.milosz.podsiadly.domain.risk.model.RiskIndicator;
import com.milosz.podsiadly.domain.risk.repository.RiskAssessmentRepository;
import com.milosz.podsiadly.domain.risk.repository.RiskIndicatorRepository;
import com.milosz.podsiadly.domain.user.model.User;
import com.milosz.podsiadly.domain.user.repository.UserRepository; // Zakładam, że masz UserRepository
import com.milosz.podsiadly.common.exception.ResourceNotFoundException; // Dodaj tę klasę, jeśli jeszcze jej nie masz
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskCalculationService {

    private final RiskIndicatorRepository riskIndicatorRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository; // Potrzebne do pobierania obiektów User

    @Transactional
    public RiskAssessment performAccountRiskAssessment(Long accountId) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));

        Map<String, BigDecimal> indicatorValues = calculateIndicatorsForAccount(account);
        RiskAssessment.RiskLevel overallRisk = determineOverallRiskLevel(indicatorValues);

        RiskAssessment assessment = RiskAssessment.builder()
                .entityType(RiskAssessment.AssessmentEntityType.ACCOUNT)
                .entityId(accountId)
                .assessedAccount(account)
                .assessmentDate(LocalDateTime.now())
                .overallRiskLevel(overallRisk)
                .indicatorValues(indicatorValues)
                .notes("Automated risk assessment for account " + accountId)
                .build();

        return riskAssessmentRepository.save(assessment);
    }

    @Transactional
    public RiskAssessment performUserRiskAssessment(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        List<BankAccount> userAccounts = bankAccountRepository.findByUserId(userId); // Zakładam taką metodę w BankAccountRepository
        Map<String, BigDecimal> indicatorValues = calculateIndicatorsForUser(user, userAccounts);
        RiskAssessment.RiskLevel overallRisk = determineOverallRiskLevel(indicatorValues);

        RiskAssessment assessment = RiskAssessment.builder()
                .entityType(RiskAssessment.AssessmentEntityType.USER)
                .entityId(userId)
                .assessedUser(user)
                .assessmentDate(LocalDateTime.now())
                .overallRiskLevel(overallRisk)
                .indicatorValues(indicatorValues)
                .notes("Automated risk assessment for user " + userId)
                .build();

        return riskAssessmentRepository.save(assessment);
    }

    // --- Private helper methods for calculations ---

    private Map<String, BigDecimal> calculateIndicatorsForAccount(BankAccount account) {
        Map<String, BigDecimal> values = new HashMap<>();

        // Example Indicator: Account Balance
        values.put("ACCOUNT_BALANCE", account.getBalance());

        // Example Indicator: Average Daily Balance over last 30 days
        // This would require more complex transaction analysis. For simplicity, let's mock it.
        // In real scenario: transactionRepository.findBySourceAccountIdAndTransactionDateBetween(...)
        values.put("AVG_DAILY_BALANCE_30D", account.getBalance().divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP)); // Mocked

        // Example Indicator: Number of large transactions in a period
        long largeTransactionsCount = transactionRepository.findBySourceAccountIdAndAmountGreaterThanAndTransactionDateBetween(
                account.getId(), BigDecimal.valueOf(10000), LocalDateTime.now().minusDays(30), LocalDateTime.now()
        ).size(); // Zakładam taką metodę w TransactionRepository
        values.put("LARGE_TRANSACTIONS_30D", BigDecimal.valueOf(largeTransactionsCount));

        // You would dynamically load RiskIndicators from repository and apply specific calculation logic
        List<RiskIndicator> allIndicators = riskIndicatorRepository.findAll();
        for (RiskIndicator indicator : allIndicators) {
            // Implement logic for each indicator type/code
            if ("DEBT_TO_INCOME_RATIO".equals(indicator.getIndicatorCode())) {
                // This would need more data, e.g., user income, loan amounts
                // For now, it's a placeholder or would be calculated for a user, not just an account
                values.put(indicator.getIndicatorCode(), BigDecimal.ZERO); // Placeholder
            }
            // Add more indicator-specific calculations here
        }

        return values;
    }

    private Map<String, BigDecimal> calculateIndicatorsForUser(User user, List<BankAccount> userAccounts) {
        Map<String, BigDecimal> values = new HashMap<>();

        // Example Indicator: Total Balance across all accounts
        BigDecimal totalBalance = userAccounts.stream()
                .map(BankAccount::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        values.put("USER_TOTAL_BALANCE", totalBalance);

        // Example Indicator: Number of active accounts
        long activeAccounts = userAccounts.stream()
                .filter(a -> a.getStatus() == BankAccount.AccountStatus.ACTIVE)
                .count();
        values.put("USER_ACTIVE_ACCOUNTS", BigDecimal.valueOf(activeAccounts));

        // You would fetch user's loan data, income data (if stored), etc., to calculate
        // indicators like Debt-to-Income ratio.
        // For now, let's add a placeholder.
        values.put("DEBT_TO_INCOME_RATIO", BigDecimal.valueOf(0.35)); // Placeholder value

        return values;
    }

    private RiskAssessment.RiskLevel determineOverallRiskLevel(Map<String, BigDecimal> indicatorValues) {
        // This is a simplified example. A real system would use a rules engine,
        // machine learning model, or a more complex scoring system.
        // It should iterate through configured RiskIndicators and check thresholds.

        boolean highRiskFound = false;
        boolean criticalRiskFound = false;

        List<RiskIndicator> configuredIndicators = riskIndicatorRepository.findAll();

        for (Map.Entry<String, BigDecimal> entry : indicatorValues.entrySet()) {
            Optional<RiskIndicator> indicatorOpt = configuredIndicators.stream()
                    .filter(i -> i.getIndicatorCode().equals(entry.getKey()))
                    .findFirst();

            if (indicatorOpt.isPresent()) {
                RiskIndicator indicator = indicatorOpt.get();
                BigDecimal value = entry.getValue();

                boolean thresholdExceeded = switch (indicator.getThresholdType()) {
                    case GREATER_THAN -> value.compareTo(BigDecimal.valueOf(indicator.getThreshold())) > 0;
                    case LESS_THAN -> value.compareTo(BigDecimal.valueOf(indicator.getThreshold())) < 0;
                    case GREATER_THAN_OR_EQUAL -> value.compareTo(BigDecimal.valueOf(indicator.getThreshold())) >= 0;
                    case LESS_THAN_OR_EQUAL -> value.compareTo(BigDecimal.valueOf(indicator.getThreshold())) <= 0;
                    case EQUALS -> value.compareTo(BigDecimal.valueOf(indicator.getThreshold())) == 0;
                    case NOT_EQUALS -> value.compareTo(BigDecimal.valueOf(indicator.getThreshold())) != 0;
                };

                if (thresholdExceeded) {
                    // This is a very simplistic mapping. In reality, each indicator
                    // might contribute to a risk score, and the score determines overall level.
                    if (indicator.getThresholdType() == RiskIndicator.ThresholdType.GREATER_THAN && value.doubleValue() > indicator.getThreshold() * 1.5) {
                        criticalRiskFound = true; // Example: significantly over threshold
                    } else {
                        highRiskFound = true;
                    }
                }
            }
        }

        if (criticalRiskFound) {
            return RiskAssessment.RiskLevel.CRITICAL;
        } else if (highRiskFound) {
            return RiskAssessment.RiskLevel.HIGH;
        } else {
            // Further logic to distinguish between MEDIUM and LOW
            // For now, let's assume if no high risk, it's MEDIUM (or LOW if all very good)
            return RiskAssessment.RiskLevel.MEDIUM; // Placeholder
        }
    }

    // You might also need methods to manage RiskIndicators themselves (create, update, delete)
    @Transactional
    public RiskIndicator createOrUpdateRiskIndicator(RiskIndicator indicator) {
        return riskIndicatorRepository.save(indicator);
    }

    @Transactional(readOnly = true)
    public Optional<RiskIndicator> getRiskIndicatorByCode(String code) {
        return riskIndicatorRepository.findByIndicatorCode(code);
    }
    @Transactional(readOnly = true)
    public List<RiskIndicator> getAllRiskIndicators() {
        return riskIndicatorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<RiskAssessment> getRiskAssessmentById(Long id) {
        return riskAssessmentRepository.findById(id);
    }
}