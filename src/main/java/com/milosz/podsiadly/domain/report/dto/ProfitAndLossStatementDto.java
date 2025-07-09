package com.milosz.podsiadly.domain.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record ProfitAndLossStatementDto(
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        BigDecimal totalRevenue,
        BigDecimal totalOperatingRevenue,
        BigDecimal totalNonOperatingRevenue,
        BigDecimal totalExpenses,
        BigDecimal totalOperatingExpenses,
        BigDecimal totalNonOperatingExpenses,
        BigDecimal grossProfit,
        BigDecimal netProfitLoss,
        Map<String, BigDecimal> revenueBreakdown, // e.g., Interest Income, Fee Income
        Map<String, BigDecimal> expenseBreakdown // e.g., Salaries, Rent, Interest Expense
) {}
