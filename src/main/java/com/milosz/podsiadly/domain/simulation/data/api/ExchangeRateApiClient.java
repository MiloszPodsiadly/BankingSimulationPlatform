package com.milosz.podsiadly.domain.simulation.data.api;

import com.milosz.podsiadly.domain.simulation.data.dto.FixerIoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Optional;

@Component
@Slf4j
public class ExchangeRateApiClient {

    private final RestTemplate restTemplate;

    @Value("${api.fixer.base-url}")
    private String baseUrl;

    @Value("${api.fixer.access-key}")
    private String accessKey;

    public ExchangeRateApiClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Optional<FixerIoResponse> getLatestExchangeRates(String baseCurrency, String symbols) {
        String url = String.format("%s?access_key=%s&base=%s&symbols=%s", baseUrl, accessKey, baseCurrency, symbols);
        log.info("Fetching latest exchange rates from: {}", url);
        try {
            FixerIoResponse response = restTemplate.getForObject(url, FixerIoResponse.class);
            if (response != null && response.success()) {
                log.info("Successfully fetched exchange rates. Base: {}, Rates count: {}", response.base(), response.rates().size());
                return Optional.of(response);
            } else if (response != null && response.error() != null) {
                log.error("Error fetching exchange rates from Fixer.io: {}", response.error());
                return Optional.empty();
            } else {
                log.warn("Fixer.io response was null or indicated failure.");
                return Optional.empty();
            }
        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching exchange rates: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error fetching exchange rates: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}