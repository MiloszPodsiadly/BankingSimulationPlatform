package com.milosz.podsiadly.domain.simulation.data.service;

// Ważne: Zmień import na rzeczywisty interfejs Fixer.io clienta z warstwy infrastrukturalnej
import com.milosz.podsiadly.infrastructure.integration.exchangerates.FixerIoClient;
// Ważne: Zmień import na rzeczywisty DTO odpowiedzi Fixer.io
import com.milosz.podsiadly.infrastructure.integration.exchangerates.dto.FixerLatestRatesResponse;
// Ważne: Upewnij się, że masz zdefiniowany ten wyjątek w common.exception
import com.milosz.podsiadly.common.exception.FixerApiException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext; // Dodany import dla MathContext
import java.util.Collections;
import java.util.List;   // Dodany import dla List
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors; // Dodany import dla Collectors

@Service
@RequiredArgsConstructor
@Slf4j
@Getter
public class ExchangeRateService {

    // --- ZMIANA 1: Zmieniamy typ wstrzykiwanego klienta API ---
    // Nazwa pola powinna odzwierciedlać nazwę interfejsu/klasy, którą wstrzykujemy.
    private final FixerIoClient fixerIoClient; // Prawidłowy typ interfejsu

    private final Map<String, BigDecimal> cachedExchangeRates = new ConcurrentHashMap<>();
    private String lastBaseCurrency = "EUR"; // Domyślna waluta bazowa dla Fixer.io (wymóg bezpłatnego planu)
    // Rozszerzone symbole, aby pokryć więcej walut
    private String lastSymbols = "PLN,USD,GBP,JPY,AUD,CAD,CHF,CNY,SEK,NZD,INR,BRL,ZAR,MXN,RUB,HKD,SGD,NOK,KRW,TRY,EUR"; // Dodano EUR na wypadek, gdyby nie było bazą

    /**
     * Fetches and caches the latest exchange rates.
     * Handles exceptions from the API client gracefully.
     * @return An unmodifiable map of currency codes to their exchange rates against the base currency.
     * Returns the current (possibly empty/stale) cache if fetching fails.
     */
    public Map<String, BigDecimal> fetchAndCacheLatestRates() {
        log.info("Attempting to fetch latest exchange rates for base {} and symbols {}", lastBaseCurrency, lastSymbols);
        try {
            // --- ZMIANA 2: Poprawne przekazanie symboli do klienta API ---
            // `fixerIoClient.getLatestRates` przyjmuje List<String>
            List<String> symbolsList = List.of(lastSymbols.split(","));
            FixerLatestRatesResponse response = fixerIoClient.getLatestRates(lastBaseCurrency, symbolsList);

            // --- ZMIANA 3: Ulepszona logika przetwarzania odpowiedzi i konwersja na BigDecimal ---
            if (response != null && response.success()) {
                if (response.rates() != null) {
                    cachedExchangeRates.clear(); // Wyczyść stary cache przed dodaniem nowych danych
                    // Konwersja Map<String, Double> z API na Map<String, BigDecimal> do cache
                    response.rates().forEach((currency, rate) -> {
                        if (rate != null) { // Sprawdź, czy kurs nie jest null
                            cachedExchangeRates.put(currency, BigDecimal.valueOf(rate));
                        }
                    });
                    log.info("Successfully cached {} exchange rates.", cachedExchangeRates.size());
                } else {
                    log.warn("Fixer.io API response was successful but contained no rates. Cache remains unchanged.");
                }
            } else {
                // Ten blok loguje błąd, jeśli API zwróci sukces=false, ale nie rzuciło wyjątku.
                // (Co nie powinno się zdarzyć, jeśli FixerIoClientImpl jest dobrze zaimplementowany,
                // bo powinien rzucać FixerApiException w przypadku success=false)
                log.warn("Failed to fetch exchange rates: Fixer.io API response indicated failure (success=false) or was malformed. Cache remains unchanged.");
            }
        } catch (FixerApiException e) {
            // --- ZMIANA 4: Poprawna obsługa wyjątku z warstwy infrastrukturalnej ---
            // Łapiemy konkretny wyjątek rzucony przez FixerIoClientImpl
            log.error("Failed to fetch exchange rates due to Fixer.io API error: {}", e.getMessage());
            // W tym przypadku cache pozostaje niezmieniony, co jest pożądanym zachowaniem
            // (użyjemy starych danych, jeśli są dostępne, lub pustego cache).
        } catch (Exception e) {
            // --- ZMIANA 5: Obsługa ogólnych nieoczekiwanych błędów ---
            // Np. problemy z siecią, nieprzewidziane wyjątki w RestTemplate, etc.
            log.error("An unexpected error occurred while fetching exchange rates: {}", e.getMessage(), e);
        }
        // Zawsze zwracamy niezmodyfikowaną mapę (bieżący cache, który mógł zostać zaktualizowany lub nie)
        return Collections.unmodifiableMap(cachedExchangeRates);
    }

    /**
     * Retrieves the latest exchange rates from cache. If cache is empty, attempts to fetch.
     * @return An unmodifiable map of currency codes to their exchange rates against the base currency.
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

    /**
     * Calculates the exchange rate between two currencies using the cached rates relative to the base currency (EUR).
     * Formula: Rate(From -> To) = Rate(EUR -> To) / Rate(EUR -> From)
     * @param fromCurrency The currency to convert from (e.g., "USD").
     * @param toCurrency The currency to convert to (e.g., "PLN").
     * @return An Optional containing the calculated exchange rate, or empty if rates are not available or calculation fails.
     */
    public Optional<BigDecimal> calculateExchangeRate(String fromCurrency, String toCurrency) {
        Map<String, BigDecimal> rates = getLatestExchangeRates(); // Upewnij się, że cache jest uzupełniony

        if (rates.isEmpty()) {
            log.warn("Cannot calculate exchange rate for {} to {}: No rates available in cache.", fromCurrency, toCurrency);
            return Optional.empty();
        }

        // Zawsze pobieramy kursy względem waluty bazowej (domyślnie EUR w Fixer.io)
        BigDecimal fromRate = rates.get(fromCurrency.toUpperCase());
        BigDecimal toRate = rates.get(toCurrency.toUpperCase());

        if (fromRate == null) {
            log.warn("Rate for fromCurrency ({}) not found in cache for conversion to {}.", fromCurrency, toCurrency);
            return Optional.empty();
        }
        if (toRate == null) {
            log.warn("Rate for toCurrency ({}) not found in cache for conversion from {}.", toCurrency, fromCurrency);
            return Optional.empty();
        }

        // Upewnij się, że nie dzielimy przez zero
        if (fromRate.compareTo(BigDecimal.ZERO) == 0) {
            log.error("From currency rate for {} is zero, cannot calculate exchange rate to {}.", fromCurrency, toCurrency);
            return Optional.empty();
        }

        // --- ZMIANA 6: Użycie MathContext dla precyzji dzielenia BigDecimal ---
        // Określ precyzję i strategię zaokrąglania
        return Optional.of(toRate.divide(fromRate, MathContext.DECIMAL64));
    }

    public void setBaseCurrency(String baseCurrency) {
        this.lastBaseCurrency = baseCurrency;
    }

    public void setSymbols(String symbols) {
        this.lastSymbols = symbols;
    }
}