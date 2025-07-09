package com.milosz.podsiadly.domain.report.service;

import com.milosz.podsiadly.domain.report.dto.BalanceSheetDto;
import com.milosz.podsiadly.domain.report.dto.ProfitAndLossStatementDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialStatementService {

    private final DataAggregator dataAggregator;

    /**
     * Generates a Profit and Loss Statement (Income Statement) for a given period.
     * @param startDate Start of the reporting period.
     * @param endDate End of the reporting period.
     * @return ProfitAndLossStatementDto containing the financial statement.
     */
    public ProfitAndLossStatementDto generateProfitAndLossStatement(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generating Profit and Loss Statement for period: {} to {}", startDate, endDate);

        Map<String, BigDecimal> pnlData = dataAggregator.aggregateProfitAndLossData(startDate, endDate);

        BigDecimal totalRevenue = pnlData.getOrDefault("totalRevenue", BigDecimal.ZERO);
        BigDecimal totalExpenses = pnlData.getOrDefault("totalExpenses", BigDecimal.ZERO);
        BigDecimal netProfitLoss = pnlData.getOrDefault("netProfitLoss", BigDecimal.ZERO);

        // This is a placeholder for actual breakdown logic.
        // In reality, this would involve detailed categorization of transactions.
        Map<String, BigDecimal> revenueBreakdown = Map.of(
                "Interest Income", totalRevenue.multiply(BigDecimal.valueOf(0.7)),
                "Fee Income", totalRevenue.multiply(BigDecimal.valueOf(0.3))
        );
        Map<String, BigDecimal> expenseBreakdown = Map.of(
                "Salaries", totalExpenses.multiply(BigDecimal.valueOf(0.5)),
                "Operating Costs", totalExpenses.multiply(BigDecimal.valueOf(0.3)),
                "Other Expenses", totalExpenses.multiply(BigDecimal.valueOf(0.2))
        );


        ProfitAndLossStatementDto statement = new ProfitAndLossStatementDto(
                startDate,
                endDate,
                totalRevenue,
                totalRevenue, // For simplicity, assuming all revenue is operating
                BigDecimal.ZERO, // No non-operating revenue in this simple example
                totalExpenses,
                totalExpenses, // For simplicity, assuming all expenses are operating
                BigDecimal.ZERO, // No non-operating expenses in this simple example
                totalRevenue.subtract(totalExpenses), // Gross profit (simplified as total revenue - total expenses)
                netProfitLoss,
                revenueBreakdown,
                expenseBreakdown
        );

        log.info("Profit and Loss Statement generated successfully for period: {} to {}", startDate, endDate);
        return statement;
    }

    /**
     * Generates a Balance Sheet for a given point in time (end of the period).
     * @return BalanceSheetDto containing the financial statement.
     */
    public BalanceSheetDto generateBalanceSheet() {
        log.info("Generating Balance Sheet as of current date.");

        Map<String, BigDecimal> balanceSheetData = dataAggregator.aggregateBalanceSheetData();

        BigDecimal totalAssets = balanceSheetData.getOrDefault("totalAssets", BigDecimal.ZERO);
        BigDecimal totalLiabilities = balanceSheetData.getOrDefault("totalLiabilities", BigDecimal.ZERO);
        BigDecimal totalEquity = balanceSheetData.getOrDefault("totalEquity", BigDecimal.ZERO);

        // Placeholder for actual breakdown logic.
        Map<String, BigDecimal> assetsBreakdown = Map.of(
                "Cash", totalAssets.multiply(BigDecimal.valueOf(0.8)),
                "Receivables", totalAssets.multiply(BigDecimal.valueOf(0.2))
        );
        Map<String, BigDecimal> liabilitiesBreakdown = Map.of(
                "Payables", totalLiabilities.multiply(BigDecimal.valueOf(0.6)),
                "Loans Payable", totalLiabilities.multiply(BigDecimal.valueOf(0.4))
        );
        Map<String, BigDecimal> equityBreakdown = Map.of(
                "Retained Earnings", totalEquity
        );


        BalanceSheetDto balanceSheet = new BalanceSheetDto(
                LocalDateTime.now(), // Report date is now
                totalAssets,
                totalLiabilities,
                totalEquity,
                assetsBreakdown,
                liabilitiesBreakdown,
                equityBreakdown
        );

        log.info("Balance Sheet generated successfully.");
        return balanceSheet;
    }
}