package com.milosz.podsiadly.domain.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FinancialSummaryDto(
        LocalDateTime summaryGeneratedAt,
        LocalDateTime periodStart,
        LocalDateTime periodEnd,
        BigDecimal totalRevenue,
        BigDecimal totalExpenses,
        BigDecimal netProfitLoss,
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        Long totalCustomers,
        Long totalActiveAccounts,
        BigDecimal averageTransactionValue
) {}