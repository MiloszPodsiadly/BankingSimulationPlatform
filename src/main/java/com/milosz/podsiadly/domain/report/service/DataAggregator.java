package com.milosz.podsiadly.domain.report.service;

import com.milosz.podsiadly.domain.bank.model.BankAccount;
import com.milosz.podsiadly.domain.bank.model.Transaction;
import com.milosz.podsiadly.domain.bank.repository.BankAccountRepository;
import com.milosz.podsiadly.domain.bank.repository.TransactionRepository;
import com.milosz.podsiadly.domain.compliance.model.AuditLog;
import com.milosz.podsiadly.domain.compliance.model.ComplianceAlert;
import com.milosz.podsiadly.domain.compliance.repository.AuditLogRepository;
import com.milosz.podsiadly.domain.compliance.repository.ComplianceAlertRepository;
import com.milosz.podsiadly.domain.report.dto.ReportRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataAggregator {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ComplianceAlertRepository complianceAlertRepository;
    // Potentially other repositories: UserRepository, LoanRepository, etc.

    /**
     * Gathers all bank accounts.
     * @return List of all BankAccount entities.
     */
    @Transactional(readOnly = true)
    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }

    /**
     * Gathers transactions within a specified time range.
     * @param startDate Start of the period.
     * @param endDate End of the period.
     * @return List of Transaction entities within the period.
     */
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByTransactionDateBetween(startDate, endDate);
    }

    /**
     * Gathers audit logs within a specified time range.
     * @param startDate Start of the period.
     * @param endDate End of the period.
     * @return List of AuditLog entities within the period.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByTimestampBetween(startDate, endDate);
    }

    /**
     * Gathers compliance alerts within a specified time range.
     * @param startDate Start of the period.
     * @param endDate End of the period.
     * @return List of ComplianceAlert entities within the period.
     */
    @Transactional(readOnly = true)
    public List<ComplianceAlert> getComplianceAlertsForPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        return complianceAlertRepository.findByCreatedAtBetween(startDate, endDate);
    }

    /**
     * Aggregates financial data for a profit and loss statement.
     * This is a simplified example; real aggregation can be very complex.
     * @param startDate Start of the period.
     * @param endDate End of the period.
     * @return A map of aggregated financial figures.
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> aggregateProfitAndLossData(LocalDateTime startDate, LocalDateTime endDate) {
        List<Transaction> transactions = getTransactionsForPeriod(startDate, endDate);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            // Simplified logic for P&L.
            // In a real system, categorization would be much more sophisticated,
            // possibly involving transaction categories, linked accounts (e.g., revenue/expense accounts), etc.

            // Example: Income from deposits or incoming transfers
            // If the transaction is a DEPOSIT, it's typically revenue to the receiving account.
            // If it's a TRANSFER, the *receiving* account sees it as an increase (revenue if from external source or loan)
            if (transaction.getType() == Transaction.TransactionType.DEPOSIT) {
                totalRevenue = totalRevenue.add(transaction.getAmount());
            }
            // Example: Expenses from withdrawals, outgoing transfers, or fees
            // If the transaction is a WITHDRAWAL, it's an expense from the 'fromAccount'.
            // If it's a TRANSFER, the *sending* account sees it as a decrease (expense).
            else if (transaction.getType() == Transaction.TransactionType.WITHDRAWAL ||
                    transaction.getType() == Transaction.TransactionType.TRANSFER ||
                    transaction.getType() == Transaction.TransactionType.FEE) {
                totalExpenses = totalExpenses.add(transaction.getAmount());
            }
            // Add more complex logic for other transaction types (e.g., LOAN_PAYMENT might reduce liabilities)
        }

        // You would also query for interest income, loan payments received, operational costs from other sources etc.
        return Map.of(
                "totalRevenue", totalRevenue,
                "totalExpenses", totalExpenses,
                "netProfitLoss", totalRevenue.subtract(totalExpenses)
        );
    }

    /**
     * Aggregates data for a balance sheet.
     * This is a simplified example; real balance sheet generation is highly complex.
     * @return A map of aggregated balance sheet figures.
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> aggregateBalanceSheetData() {
        List<BankAccount> allAccounts = getAllBankAccounts();

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (BankAccount account : allAccounts) {
            if (account.getStatus() == BankAccount.AccountStatus.ACTIVE) {
                // Assuming positive balances are assets, negative (e.g., overdrafts, loans) are liabilities
                if (account.getBalance().compareTo(BigDecimal.ZERO) >= 0) {
                    totalAssets = totalAssets.add(account.getBalance());
                } else {
                    // Treat negative balances as liabilities
                    totalLiabilities = totalLiabilities.add(account.getBalance().negate());
                }
            }
        }

        // In a real system, you'd also consider:
        // - Other asset types: investments, fixed assets, accrued interest receivable
        // - Other liability types: bonds payable, accrued interest payable, unearned revenue
        // - Inter-company accounts for proper consolidation
        BigDecimal totalEquity = totalAssets.subtract(totalLiabilities); // Simplified: Assets = Liabilities + Equity

        return Map.of(
                "totalAssets", totalAssets,
                "totalLiabilities", totalLiabilities,
                "totalEquity", totalEquity
        );
    }

    // You could add more specific aggregation methods here, e.g.,
    // public Map<String, Long> countAuditEventsByActionType(LocalDateTime startDate, LocalDateTime endDate) { ... }
    // public Map<String, Long> countAlertsBySeverity(LocalDateTime startDate, LocalDateTime endDate) { ... }
}