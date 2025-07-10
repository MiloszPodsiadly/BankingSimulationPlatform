package com.milosz.podsiadly.domain.simulation.data.service;

// --- ZMIANA 1: Poprawny import interfejsu klienta WorldBankClient ---
import com.milosz.podsiadly.infrastructure.integration.worldbank.WorldBankClient; // Zmieniono z WorldBankApiClient na WorldBankClient!
// --- ZMIANA 2: Import WorldBankResponse DTO ---
import com.milosz.podsiadly.infrastructure.integration.worldbank.dto.WorldBankResponse;
import com.milosz.podsiadly.infrastructure.integration.worldbank.dto.WorldBankIndicatorData;
import com.milosz.podsiadly.common.exception.WorldBankApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EconomicDataService {

    // --- ZMIANA 3: Wstrzykiwanie WorldBankClient (spójne z WorldBankClientImpl) ---
    private final WorldBankClient worldBankClient;
    // Cache for latest fetched economic indicators
    // Key: "countryCode_indicatorCode", Value: WorldBankIndicatorData (cały rekord, aby mieć dostęp do daty)
    private final Map<String, WorldBankIndicatorData> cachedEconomicData = new ConcurrentHashMap<>();

    // Default indicators to fetch
    private final List<EconomicIndicatorConfig> defaultIndicators = List.of(
            new EconomicIndicatorConfig("PL", "FP.CPI.TOTL.ZG", "Inflation - Poland"), // Polska inflacja (roczna %)
            new EconomicIndicatorConfig("US", "FP.CPI.TOTL.ZG", "Inflation - USA"),   // USA inflacja (roczna %)
            new EconomicIndicatorConfig("PL", "NY.GDP.MKTP.CD", "GDP - Poland"),      // Polska PKB (current USD)
            new EconomicIndicatorConfig("US", "NY.GDP.MKTP.CD", "GDP - USA"),           // USA PKB (current USD)
            new EconomicIndicatorConfig("PL", "SL.UEM.TOTL.NE.ZS", "Unemployment - Poland"), // Stopa bezrobocia (%), szacunki MOP
            new EconomicIndicatorConfig("US", "SL.UEM.TOTL.NE.ZS", "Unemployment - USA")    // Stopa bezrobocia (%), szacunki MOP
    );

    /**
     * Fetches and caches specific economic indicators, prioritizing the latest available year.
     * World Bank API often does not have data for the current year.
     * @param countryCode ISO 2-letter country code.
     * @param indicatorCode World Bank indicator code.
     * @param yearsToFetch Number of recent years to attempt fetching data for (e.g., 5 to get data for last 5 years).
     * @return An Optional containing the most recent fetched WorldBankIndicatorData, if successful.
     */
    public Optional<WorldBankIndicatorData> fetchAndCacheEconomicIndicator(
            String countryCode, String indicatorCode, int yearsToFetch) {

        String cacheKey = countryCode + "_" + indicatorCode;
        log.info("Attempting to fetch economic data for {} - {}", countryCode, indicatorCode);

        try {
            int currentYear = LocalDate.now().getYear();
            String yearRange = (currentYear - yearsToFetch + 1) + ":" + currentYear;

            // --- ZMIANA 4: Wywołanie metody getIndicatorData (spójne z WorldBankClientImpl) ---
            // Zwracamy WorldBankResponse, nie bezpośrednio listę danych
            WorldBankResponse apiResponse = worldBankClient.getIndicatorData(
                    countryCode, indicatorCode, "json", yearRange);

            // --- ZMIANA 5: Pobranie listy danych z WorldBankResponse ---
            List<WorldBankIndicatorData> dataList = apiResponse.indicatorData();

            if (!dataList.isEmpty()) {
                Optional<WorldBankIndicatorData> latestDataOpt = dataList.stream()
                        .filter(data -> data.date() != null && data.value() != null)
                        .max(Comparator.comparing(data -> {
                            try {
                                return Integer.parseInt(data.date());
                            } catch (NumberFormatException e) {
                                log.warn("Invalid date format for indicator {}-{}: '{}'. Skipping this data point.",
                                        countryCode, indicatorCode, data.date());
                                return 0;
                            }
                        }));

                if (latestDataOpt.isPresent()) {
                    WorldBankIndicatorData latestData = latestDataOpt.get();
                    cachedEconomicData.put(cacheKey, latestData);
                    log.info("Successfully cached economic data for {}-{}. Latest value for year {}: {}",
                            countryCode, indicatorCode, latestData.date(), latestData.value());
                    return Optional.of(latestData);
                } else {
                    log.warn("Fetched data for {}-{} was empty or contained no valid values after filtering. Cache remains unchanged.", countryCode, indicatorCode);
                    return Optional.empty();
                }
            } else {
                // Dodatkowe logowanie, jeśli API zwraca pustą listę danych (total=0 w metadanych)
                if (apiResponse.metadata().isPresent() && apiResponse.metadata().get().total() != null && apiResponse.metadata().get().total() == 0) {
                    log.info("No data found for indicator {} in country {} (total results 0). Cache remains unchanged.", indicatorCode, countryCode);
                } else {
                    log.warn("No economic data found for {}-{}. Cache remains unchanged.", countryCode, indicatorCode);
                }
                return Optional.empty();
            }
        } catch (WorldBankApiException e) {
            log.error("Failed to fetch economic data for {}-{} due to World Bank API error: {}. Cache remains unchanged.",
                    countryCode, indicatorCode, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("An unexpected error occurred while fetching economic data for {}-{}: {}. Cache remains unchanged.",
                    countryCode, indicatorCode, e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Fetches and caches all default economic indicators.
     * This method will be called by the scheduler.
     * Data for the last 5 years is attempted to be fetched, and the latest available is cached.
     */
    public void fetchAndCacheAllDefaultIndicators() {
        log.info("Fetching and caching all default economic indicators.");
        // Opcjonalnie: cachedEconomicData.clear(); // Czyść tylko jeśli chcesz zawsze świeży start

        for (EconomicIndicatorConfig config : defaultIndicators) {
            fetchAndCacheEconomicIndicator(config.countryCode, config.indicatorCode, 5);
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
                .map(WorldBankIndicatorData::value)
                .filter(value -> value != null && !value.isBlank())
                .map(BigDecimal::new);
    }

    // Helper record for default indicator configuration
    private record EconomicIndicatorConfig(String countryCode, String indicatorCode, String description) {}
}