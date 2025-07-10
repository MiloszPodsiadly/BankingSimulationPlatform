package com.milosz.podsiadly.infrastructure.integration.exchangerates;

import com.milosz.podsiadly.infrastructure.integration.exchangerates.dto.FixerLatestRatesResponse;

import java.util.List;

public interface FixerIoClient {

    /**
     * Pobiera najnowsze kursy wymiany walut z Fixer.io dla określonej waluty bazowej
     * i listy walut docelowych.
     *
     * @param baseCurrency Waluta bazowa (np. "EUR", "USD").
     * @param targetCurrencies Lista walut docelowych (np. ["USD", "GBP", "PLN"]). Może być null/pusta dla wszystkich.
     * @return Obiekt zawierający najnowsze kursy wymiany.
     */
    FixerLatestRatesResponse getLatestRates(String baseCurrency, List<String> targetCurrencies);

    /**
     * Pobiera najnowszy kurs wymiany z jednej waluty na drugą.
     * Jest to metoda pomocnicza, która może wykorzystywać getLatestRates.
     *
     * @param fromCurrency Waluta źródłowa.
     * @param toCurrency Waluta docelowa.
     * @return Kurs wymiany.
     * @throws RuntimeException jeśli kurs nie zostanie znaleziony lub wystąpi błąd API.
     */
    Double getExchangeRate(String fromCurrency, String toCurrency);
}