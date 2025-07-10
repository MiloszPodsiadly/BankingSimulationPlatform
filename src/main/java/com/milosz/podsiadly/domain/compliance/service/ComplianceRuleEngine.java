package com.milosz.podsiadly.domain.compliance.service;

import com.milosz.podsiadly.domain.compliance.model.AuditLog;
import com.milosz.podsiadly.domain.compliance.model.ComplianceAlert;
import com.milosz.podsiadly.domain.compliance.model.ComplianceAlert.AlertSeverity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component; // Use @Component or @Service
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A rule engine for evaluating complex compliance rules based on audit logs.
 * This can be extended with external rule engines (e.g., Drools) or custom rule definitions.
 */
@Component // Can be @Service as well, @Component is more generic
@Slf4j
public class ComplianceRuleEngine {

    // Define a functional interface for a compliance rule
    // Each rule takes an AuditLog and returns a ComplianceAlert if triggered, otherwise null
    public interface ComplianceRule extends Function<AuditLog, ComplianceAlert> {}

    private final List<ComplianceRule> rules = new ArrayList<>();
    // Constructor or @PostConstruct to add rules
    public ComplianceRuleEngine() {
        // Example rule: Detect transaction amounts above a certain threshold (if not caught by simple check)
        rules.add(auditLog -> {
            if ("TRANSACTION_CREATED".equals(auditLog.getAction()) && auditLog.getDetails() != null) {
                try {
                    // This parsing logic should be robust in a real application
                    BigDecimal amount = new BigDecimal(auditLog.getDetails().split("amount\":")[1].split(",")[0].trim());
                    if (amount.compareTo(new BigDecimal("50000.00")) > 0) { // Very large transaction
                        return ComplianceAlert.builder()
                                .alertCode("SUPER_LARGE_TRANSACTION")
                                .description("Transaction of " + amount + " by " + auditLog.getUsername() + " is extremely large.")
                                .severity(AlertSeverity.CRITICAL)
                                .triggeredByEntityType("Transaction")
                                .triggeredByEntityId(auditLog.getEntityId())
                                .relatedDetails(auditLog.getDetails())
                                .build();
                    }
                } catch (Exception e) {
                    log.error("Rule engine: Failed to parse transaction amount from audit log details: {}", auditLog.getDetails(), e);
                }
            }
            return null; // Rule not triggered
        });

        // Example rule: Detect rapid sequence of different actions from same user (e.g., login, then transfer, then logout quickly)
        // This would require stateful tracking, which is more complex and might use a dedicated stream processing engine.
        // For simplicity, this example just demonstrates adding another rule placeholder.
        rules.add(auditLog -> {
            // Placeholder for a more complex, stateful rule
            // e.g., analyze sequence of audit logs for a user within a time window
            if ("ACCOUNT_ACCESSED".equals(auditLog.getAction())) {
                // Imagine logic that checks if this user also did a "LOAN_REQUESTED" within 10 seconds.
                // For this to work robustly, you'd need a cache/database of recent audit logs.
            }
            return null;
        });

        // Add more rules as needed
    }

    /**
     * Evaluates all defined compliance rules against a given audit log.
     * @param auditLog The audit log to evaluate.
     * @return A list of ComplianceAlerts triggered by the audit log.
     */
    public List<ComplianceAlert> evaluateRules(AuditLog auditLog) {
        List<ComplianceAlert> triggeredAlerts = new ArrayList<>();
        for (ComplianceRule rule : rules) {
            ComplianceAlert alert = rule.apply(auditLog);
            if (alert != null) {
                triggeredAlerts.add(alert);
            }
        }
        return triggeredAlerts;
    }
}