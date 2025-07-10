package com.milosz.podsiadly.infrastructure.integration.exchangerates;

import com.milosz.podsiadly.infrastructure.integration.exchangerates.dto.FixerLatestRatesResponse;
import com.milosz.podsiadly.common.exception.FixerApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FixerIoClientImpl implements FixerIoClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String accessKey;

    public FixerIoClientImpl(
            RestTemplate restTemplate,
            @Value("${api.fixer.base-url}") String baseUrl,
            @Value("${api.fixer.access-key}") String accessKey
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.accessKey = accessKey;
    }

    @Override
    public FixerLatestRatesResponse getLatestRates(String baseCurrency, List<String> targetCurrencies) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl + "/latest")
                .queryParam("access_key", accessKey);

        if (baseCurrency != null && !baseCurrency.isBlank()) {
            uriBuilder.queryParam("base", baseCurrency.toUpperCase());
        }

        if (targetCurrencies != null && !targetCurrencies.isEmpty()) {
            String symbols = targetCurrencies.stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.joining(","));
            uriBuilder.queryParam("symbols", symbols);
        }

        String url = uriBuilder.toUriString();
        log.info("Calling Fixer.io API: {}", url);

        try {
            FixerLatestRatesResponse response = restTemplate.getForObject(url, FixerLatestRatesResponse.class);

            // Zmiana tutaj: używamy response.success() i response.error()
            if (response != null && !response.success()) {
                String errorMessage = Optional.ofNullable(response.error())
                        .map(error -> String.format("Fixer.io API Error [Code: %d, Type: %s, Info: %s]",
                                error.code(), error.type(), error.info())) // Zmiana tutaj
                        .orElse("Unknown Fixer.io API Error (success=false but no error details)");
                log.error(errorMessage);
                throw new FixerApiException(errorMessage);
            }

            if (response == null) {
                String message = "Fixer.io API returned null response.";
                log.error(message);
                throw new FixerApiException(message);
            }

            return response;

        } catch (HttpClientErrorException e) {
            String errorMessage = String.format("HTTP Error from Fixer.io API: %s - %s", e.getStatusCode(), e.getResponseBodyAsString());
            log.error(errorMessage, e);
            throw new FixerApiException(errorMessage);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to call Fixer.io API or parse response: %s", e.getMessage());
            log.error(errorMessage, e);
            throw new FixerApiException(errorMessage);
        }
    }

    @Override
    public Double getExchangeRate(String fromCurrency, String toCurrency) {
        if (fromCurrency == null || fromCurrency.isBlank() || toCurrency == null || toCurrency.isBlank()) {
            throw new IllegalArgumentException("From and to currency cannot be null or empty.");
        }

        FixerLatestRatesResponse response = getLatestRates("EUR", List.of(fromCurrency, toCurrency));

        // Zmiana tutaj: używamy response.rates()
        Map<String, Double> rates = response.rates();
        if (rates == null || rates.isEmpty()) {
            String message = "No rates found in Fixer.io response for " + fromCurrency + " to " + toCurrency;
            log.error(message);
            throw new FixerApiException(message);
        }

        Double fromRate = rates.get(fromCurrency.toUpperCase());
        Double toRate = rates.get(toCurrency.toUpperCase());

        if (fromRate == null || toRate == null) {
            String message = String.format("Could not find rates for %s or %s in Fixer.io response.", fromCurrency, toCurrency);
            log.error(message);
            throw new FixerApiException(message);
        }

        if ("EUR".equalsIgnoreCase(fromCurrency)) {
            return toRate;
        } else if ("EUR".equalsIgnoreCase(toCurrency)) {
            return 1.0 / fromRate;
        } else {
            return toRate / fromRate;
        }
    }
}