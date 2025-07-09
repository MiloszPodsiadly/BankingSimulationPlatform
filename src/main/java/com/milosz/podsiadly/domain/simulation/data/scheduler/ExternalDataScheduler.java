package com.milosz.podsiadly.domain.simulation.data.scheduler;

import com.milosz.podsiadly.domain.simulation.data.service.EconomicDataService;
import com.milosz.podsiadly.domain.simulation.data.service.ExchangeRateService;
import com.milosz.podsiadly.domain.simulation.data.service.FinancialNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalDataScheduler {

    private final ExchangeRateService exchangeRateService;
    private final FinancialNewsService financialNewsService;
    private final EconomicDataService economicDataService;

    /**
     * Scheduled task to fetch and cache external data (exchange rates, news, economic data).
     * Runs every 30 minutes.
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes in milliseconds
    public void fetchAllExternalData() {
        log.info("Starting scheduled external data fetch at {}", System.currentTimeMillis());

        try {
            exchangeRateService.fetchAndCacheLatestRates();
        } catch (Exception e) {
            log.error("Error fetching exchange rates: {}", e.getMessage(), e);
        }

        try {
            // Default query for news. Can be made configurable.
            financialNewsService.fetchAndCacheLatestNews("financial news", 20, 1);
        } catch (Exception e) {
            log.error("Error fetching financial news: {}", e.getMessage(), e);
        }

        try {
            economicDataService.fetchAndCacheAllDefaultIndicators();
        } catch (Exception e) {
            log.error("Error fetching economic data: {}", e.getMessage(), e);
        }

        log.info("Finished scheduled external data fetch.");
    }
}