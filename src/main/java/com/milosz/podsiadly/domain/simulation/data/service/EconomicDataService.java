package com.milosz.podsiadly.domain.simulation.data.service;

import com.milosz.podsiadly.domain.simulation.data.api.EconomicDataApiClient;
import com.milosz.podsiadly.domain.simulation.data.dto.WorldBankData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EconomicDataService {

    private final EconomicDataApiClient economicDataApiClient;
    // Cache for latest fetched economic indicators
    // Key: "countryCode_indicatorCode", Value: WorldBankData.IndicatorData
    private final Map<String, WorldBankData.IndicatorData> cachedEconomicData = new ConcurrentHashMap<>();

    // Default indicators to fetch
    private final List<EconomicIndicatorConfig> defaultIndicators = List.of(
            new EconomicIndicatorConfig("PL", "FP.CPI.TOTL.ZG", "Inflation - Poland"), // Polska inflacja
            new EconomicIndicatorConfig("US", "FP.CPI.TOTL.ZG", "Inflation - USA"),   // USA inflacja
            new EconomicIndicatorConfig("PL", "NY.GDP.MKTP.CD", "GDP - Poland"),      // Polska PKB
            new EconomicIndicatorConfig("US", "NY.GDP.MKTP.CD", "GDP - USA")           // USA PKB
            // Add more as needed
    );

    /**
     * Fetches and caches specific economic indicators.
     * @param countryCode ISO 2-letter country code.
     * @param indicatorCode World Bank indicator code.
     * @param yearRange E.g., "2020:2023" or "2023"
     * @return The fetched IndicatorData, if successful.
     */
    public Optional<WorldBankData.IndicatorData> fetchAndCacheEconomicIndicator(
            String countryCode, String indicatorCode, String yearRange) {

        String cacheKey = countryCode + "_" + indicatorCode;
        log.info("Attempting to fetch economic data for {} - {}", countryCode, indicatorCode);

        Optional<WorldBankData.IndicatorData> dataOpt = economicDataApiClient.getEconomicIndicatorData(
                countryCode, indicatorCode, "json", yearRange);

        if (dataOpt.isPresent()) {
            cachedEconomicData.put(cacheKey, dataOpt.get());
            log.info("Successfully cached economic data for {}-{}.", countryCode, indicatorCode);
        } else {
            log.warn("Failed to fetch economic data for {}-{}.", countryCode, indicatorCode);
        }
        return dataOpt;
    }

    /**
     * Fetches and caches all default economic indicators.
     * This method will be called by the scheduler.
     */
    public void fetchAndCacheAllDefaultIndicators() {
        log.info("Fetching and caching all default economic indicators.");
        cachedEconomicData.clear(); // Clear old cache for a full refresh
        String currentYear = String.valueOf(LocalDate.now().getYear());

        for (EconomicIndicatorConfig config : defaultIndicators) {
            fetchAndCacheEconomicIndicator(config.countryCode, config.indicatorCode, currentYear);
        }
        log.info("Finished fetching and caching default economic indicators. Cached {} entries.", cachedEconomicData.size());
    }

    /**
     * Retrieves the latest value for a specific economic indicator from cache.
     * @param countryCode ISO 2-letter country code.
     * @param indicatorCode World Bank indicator code.
     * @return An Optional containing the BigDecimal value, or empty if not found.
     */
    public Optional<BigDecimal> getCachedIndicatorValue(String countryCode, String indicatorCode) {
        String cacheKey = countryCode + "_" + indicatorCode;
        return Optional.ofNullable(cachedEconomicData.get(cacheKey))
                .map(WorldBankData.IndicatorData::value);
    }

    // Helper record for default indicator configuration
    private record EconomicIndicatorConfig(String countryCode, String indicatorCode, String description) {}
}