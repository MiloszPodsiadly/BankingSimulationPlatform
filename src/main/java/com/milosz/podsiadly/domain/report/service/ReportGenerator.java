package com.milosz.podsiadly.domain.report.service;

import com.milosz.podsiadly.domain.compliance.dto.AuditLogEntryDto;
import com.milosz.podsiadly.domain.compliance.dto.AuditReportDto;
import com.milosz.podsiadly.domain.compliance.dto.ComplianceAlertDto;
import com.milosz.podsiadly.domain.compliance.dto.ComplianceReportDto;
import com.milosz.podsiadly.domain.compliance.model.AuditLog;
import com.milosz.podsiadly.domain.compliance.model.ComplianceAlert;
import com.milosz.podsiadly.domain.compliance.service.AuditService;
import com.milosz.podsiadly.domain.compliance.service.ComplianceMonitoringService;
import com.milosz.podsiadly.domain.report.dto.*;
import com.milosz.podsiadly.domain.report.model.BankStatement;
import com.milosz.podsiadly.domain.report.repository.BankStatementRepository;
import com.milosz.podsiadly.common.exception.ReportGenerationException; // You might need to create this custom exception
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerator {

    private final DataAggregator dataAggregator;
    private final FinancialStatementService financialStatementService;
    private final AuditService auditService; // To get audit logs for reports
    private final ComplianceMonitoringService complianceMonitoringService; // To get compliance alerts for reports
    private final BankStatementRepository bankStatementRepository; // To save generated statements

    /**
     * Generates a report based on the provided request.
     * @param requestDto The ReportRequestDto specifying the type and parameters of the report.
     * @return An Object representing the generated report DTO (can be cast based on type).
     * @throws ReportGenerationException if the report type is not supported or generation fails.
     */
    @Transactional
    public Object generateReport(ReportRequestDto requestDto) {
        log.info("Generating report of type {} for period {} to {}", requestDto.reportType(), requestDto.startDate(), requestDto.endDate());

        Object generatedReport;
        switch (requestDto.reportType()) {
            case BALANCE_SHEET:
                generatedReport = financialStatementService.generateBalanceSheet();
                break;
            case PROFIT_AND_LOSS:
                generatedReport = financialStatementService.generateProfitAndLossStatement(requestDto.startDate(), requestDto.endDate());
                break;
            case AUDIT_SUMMARY:
                generatedReport = generateAuditSummaryReport(requestDto.startDate(), requestDto.endDate());
                break;
            case TRANSACTION_HISTORY:
                generatedReport = generateTransactionHistoryReport(requestDto.startDate(), requestDto.endDate(), requestDto.accountId());
                break;
            case COMPLIANCE_ALERTS_SUMMARY:
                generatedReport = generateComplianceAlertsSummaryReport(requestDto.startDate(), requestDto.endDate());
                break;
            case FINANCIAL_SUMMARY:
                generatedReport = generateOverallFinancialSummary(requestDto.startDate(), requestDto.endDate());
                break;
            default:
                throw new ReportGenerationException("Unsupported report type: " + requestDto.reportType());
        }

        // Optional: Save report content to BankStatement if it's a "physical" report like a bank statement
        // This part needs more specific logic depending on what 'content' you want to save.
        if (requestDto.reportType() == ReportRequestDto.ReportType.TRANSACTION_HISTORY && requestDto.accountId() != null) {
            BankStatement statement = BankStatement.builder()
                    .accountId(requestDto.accountId())
                    .statementReference(UUID.randomUUID().toString())
                    .periodStart(requestDto.startDate())
                    .periodEnd(requestDto.endDate())
                    .statementType("TRANSACTION_HISTORY")
                    // You'd convert the generatedReport (e.g., List<TransactionDto>) to JSON string here
                    .content("Generated transaction history for account " + requestDto.accountId()) // Placeholder
                    .status(BankStatement.StatementStatus.GENERATED)
                    .build();
            bankStatementRepository.save(statement);
            log.info("Bank statement for account {} saved with reference {}", requestDto.accountId(), statement.getStatementReference());
        }

        log.info("Report type {} generated successfully.", requestDto.reportType());
        return generatedReport;
    }

    private AuditReportDto generateAuditSummaryReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<AuditLog> auditLogs = auditService.getAuditLogsByTimestampRange(startDate, endDate);

        long totalEntries = auditLogs.size();
        long successfulEntries = auditLogs.stream().filter(log -> log.getStatus() == AuditLog.AuditStatus.SUCCESS).count();
        long failedEntries = totalEntries - successfulEntries;

        // Assuming you have an AuditMapper to convert AuditLog to AuditLogEntryDto
        // You'll need to inject AuditMapper here if you want to use it
        // For simplicity, directly constructing AuditReportDto in ComplianceController,
        // but it makes more sense to have this logic here or in a dedicated AuditReportService.
        // For now, let's just count and not return full DTOs for simplicity of this method.
        // If you want full DTOs, you'd inject AuditMapper and use it.
        return new AuditReportDto(
                LocalDateTime.now(),
                startDate,
                endDate,
                totalEntries,
                successfulEntries,
                failedEntries,
                auditLogs.stream().collect(Collectors.groupingBy(AuditLog::getAction, Collectors.counting())),
                auditLogs.stream()
                        .filter(log -> log.getStatus() == AuditLog.AuditStatus.FAILURE) // Simplified critical failed
                        .map(log -> new AuditLogEntryDto(log.getId(), log.getUsername(), log.getAction(), log.getEntityType(), log.getEntityId(), log.getDetails(), log.getTimestamp(), log.getStatus())) // Manual mapping for example
                        .collect(Collectors.toList()),
                auditLogs.stream()
                        .map(log -> new AuditLogEntryDto(log.getId(), log.getUsername(), log.getAction(), log.getEntityType(), log.getEntityId(), log.getDetails(), log.getTimestamp(), log.getStatus())) // Manual mapping for example
                        .collect(Collectors.toList())
        );
    }

    private ComplianceReportDto generateComplianceAlertsSummaryReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<ComplianceAlert> alerts = complianceMonitoringService.getComplianceAlertsForPeriod(startDate, endDate);

        long totalAlerts = alerts.size();
        long openAlerts = alerts.stream().filter(a -> a.getStatus() == ComplianceAlert.AlertStatus.OPEN).count();
        long resolvedAlerts = alerts.stream().filter(a -> a.getStatus() == ComplianceAlert.AlertStatus.RESOLVED).count();
        long dismissedAlerts = alerts.stream().filter(a -> a.getStatus() == ComplianceAlert.AlertStatus.DISMISSED).count();

        return new ComplianceReportDto(
                LocalDateTime.now(),
                startDate,
                endDate,
                totalAlerts,
                openAlerts,
                resolvedAlerts,
                dismissedAlerts,
                alerts.stream().collect(Collectors.groupingBy(alert -> alert.getSeverity().name(), Collectors.counting())),
                alerts.stream()
                        .filter(a -> a.getStatus() == ComplianceAlert.AlertStatus.OPEN)
                        .map(a -> new ComplianceAlertDto(a.getId(), a.getAlertCode(), a.getDescription(), a.getSeverity(), a.getStatus(), a.getTriggeredByEntityType(), a.getTriggeredByEntityId(), a.getRelatedDetails(), a.getCreatedAt(), a.getResolvedAt(), a.getResolvedBy())) // Manual mapping
                        .collect(Collectors.toList()),
                null // ruleTriggerCounts - requires more advanced tracking
        );
    }

    private List<Object> generateTransactionHistoryReport(LocalDateTime startDate, LocalDateTime endDate, Long accountId) {
        // This method would ideally return a List<TransactionDto>
        // For simplicity, we just return aggregated raw data for now.
        // You'd need a TransactionService and TransactionMapper to convert to DTOs.
        return dataAggregator.getTransactionsForPeriod(startDate, endDate).stream()
                .filter(t -> accountId == null || t.getSourceAccount().getId().equals(accountId) || t.getTargetAccount().getId().equals(accountId))
                // You'd map Transaction entity to TransactionDto here
                .map(t -> (Object) t.getTransactionRef() + " - " + t.getAmount() + " " + t.getCurrency()) // Placeholder
                .collect(Collectors.toList());
    }

    private FinancialSummaryDto generateOverallFinancialSummary(LocalDateTime startDate, LocalDateTime endDate) {
        // This is a high-level summary that can combine P&L and Balance Sheet insights
        ProfitAndLossStatementDto pnl = financialStatementService.generateProfitAndLossStatement(startDate, endDate);
        BalanceSheetDto bs = financialStatementService.generateBalanceSheet(); // As of current date

        return new FinancialSummaryDto(
                LocalDateTime.now(),
                startDate,
                endDate,
                pnl.totalRevenue(),
                pnl.totalExpenses(),
                pnl.netProfitLoss(),
                bs.totalAssets(),
                bs.totalLiabilities(),
                // Placeholder for total customers/active accounts - would need more data aggregation
                null,
                null,
                BigDecimal.ZERO // Placeholder for average transaction value
        );
    }
}