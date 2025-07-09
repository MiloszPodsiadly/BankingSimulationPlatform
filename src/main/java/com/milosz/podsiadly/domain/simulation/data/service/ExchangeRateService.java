package com.milosz.podsiadly.domain.simulation.data.service;

import com.milosz.podsiadly.domain.simulation.data.api.ExchangeRateApiClient;
import com.milosz.podsiadly.domain.simulation.data.dto.FixerIoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final ExchangeRateApiClient exchangeRateApiClient;
    private final Map<String, BigDecimal> cachedExchangeRates = new ConcurrentHashMap<>();
    private String lastBaseCurrency = "EUR"; // Default base currency for Fixer.io
    private String lastSymbols = "PLN,USD,GBP,JPY"; // Default symbols to fetch

    /**
     * Fetches and caches the latest exchange rates.
     * @return A map of currency codes to their exchange rates against the base currency.
     */
    public Map<String, BigDecimal> fetchAndCacheLatestRates() {
        log.info("Attempting to fetch latest exchange rates for base {} and symbols {}", lastBaseCurrency, lastSymbols);
        Optional<FixerIoResponse> responseOpt = exchangeRateApiClient.getLatestExchangeRates(lastBaseCurrency, lastSymbols);

        if (responseOpt.isPresent()) {
            FixerIoResponse response = responseOpt.get();
            if (response.success() && response.rates() != null) {
                cachedExchangeRates.clear(); // Clear old cache
                cachedExchangeRates.putAll(response.rates());
                log.info("Successfully cached {} exchange rates.", cachedExchangeRates.size());
                return Collections.unmodifiableMap(cachedExchangeRates);
            } else {
                log.warn("Failed to fetch exchange rates: API response indicated failure or no rates.");
            }
        } else {
            log.warn("Failed to fetch exchange rates: No response from API client.");
        }
        return Collections.unmodifiableMap(cachedExchangeRates); // Return current (possibly empty/stale) cache
    }

    /**
     * Retrieves the latest exchange rates from cache. If cache is empty, attempts to fetch.
     * @return A map of currency codes to their exchange rates against the base currency.
     */
    public Map<String, BigDecimal> getLatestExchangeRates() {
        if (cachedExchangeRates.isEmpty()) {
            log.warn("Exchange rate cache is empty. Attempting to fetch now.");
            return fetchAndCacheLatestRates();
        }
        return Collections.unmodifiableMap(cachedExchangeRates);
    }

    /**
     * Get a specific exchange rate from the cache.
     * @param targetCurrency The currency to get the rate for (e.g., "PLN").
     * @return An Optional containing the exchange rate, or empty if not found.
     */
    public Optional<BigDecimal> getRate(String targetCurrency) {
        return Optional.ofNullable(cachedExchangeRates.get(targetCurrency.toUpperCase()));
    }

    // You can add methods to set base currency and symbols if needed for dynamic configuration
    public void setBaseCurrency(String baseCurrency) {
        this.lastBaseCurrency = baseCurrency;
    }

    public void setSymbols(String symbols) {
        this.lastSymbols = symbols;
    }
}