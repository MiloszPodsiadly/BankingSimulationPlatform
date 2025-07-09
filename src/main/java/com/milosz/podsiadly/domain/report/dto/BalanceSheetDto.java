package com.milosz.podsiadly.domain.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record BalanceSheetDto(
        LocalDateTime reportDate,
        BigDecimal totalAssets,
        BigDecimal totalLiabilities,
        BigDecimal totalEquity,
        Map<String, BigDecimal> assetsBreakdown, // e.g., Cash, Receivables, Fixed Assets
        Map<String, BigDecimal> liabilitiesBreakdown, // e.g., Payables, Loans, Bonds
        Map<String, BigDecimal> equityBreakdown // e.g., Share Capital, Retained Earnings
) {}
