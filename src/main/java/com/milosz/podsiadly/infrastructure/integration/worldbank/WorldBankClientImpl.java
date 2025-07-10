package com.milosz.podsiadly.infrastructure.integration.worldbank;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper; // Potrzebny do ręcznego mapowania JSON
import com.milosz.podsiadly.infrastructure.integration.worldbank.dto.WorldBankIndicatorData;
import com.milosz.podsiadly.infrastructure.integration.worldbank.dto.WorldBankMetadata;
import com.milosz.podsiadly.common.exception.WorldBankApiException;
import com.milosz.podsiadly.infrastructure.integration.worldbank.dto.WorldBankResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class WorldBankClientImpl implements WorldBankClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper objectMapper; // Będzie potrzebny do ręcznej deserializacji skomplikowanej odpowiedzi

    public WorldBankClientImpl(
            @Qualifier("worldBankRestTemplate") RestTemplate restTemplate, // Pamiętaj o kwalifikatorze
            @Value("${api.worldbank.base-url}") String baseUrl,
            ObjectMapper objectMapper // Jackson's ObjectMapper jest automatycznie beanem w Spring Boot
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
    }

    @Override
    public WorldBankResponse getIndicatorData(String countryCode, String indicatorId, String dateFrom, String dateTo) {
        if (countryCode == null || countryCode.isBlank() || indicatorId == null || indicatorId.isBlank()) {
            throw new IllegalArgumentException("Country code and indicator ID cannot be null or empty.");
        }

        // World Bank API używa formatu JSONP domyślnie, chcemy czysty JSON.
        // Parametr "format=json" jest kluczowy!
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(baseUrl + "/v2/country/{countryCode}/indicator/{indicatorId}")
                .queryParam("format", "json")
                .encode(); // Ważne dla kodowania URL-i, np. spacji w parametrach

        if (dateFrom != null && !dateFrom.isBlank()) {
            uriBuilder.queryParam("date", dateFrom + (dateTo != null && !dateTo.isBlank() ? ":" + dateTo : ""));
        } else if (dateTo != null && !dateTo.isBlank()) {
            // Jeśli tylko dateTo, to API może interpretować to jako zakres do dateTo
            uriBuilder.queryParam("date", dateTo);
        }

        String url = uriBuilder.buildAndExpand(countryCode, indicatorId).toUriString();
        log.info("Calling World Bank API: {}", url);

        try {
            // World Bank API zwraca List<Object>, gdzie:
            // - pierwszy element to metadane (Map),
            // - drugi element to lista danych (List<Map>).
            // Musimy obsłużyć to "ręcznie", więc używamy ParameterizedTypeReference z exchange().
            ResponseEntity<List<List<Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            List<List<Object>> rawResponse = response.getBody();

            if (rawResponse == null || rawResponse.isEmpty()) {
                log.warn("World Bank API returned empty or null response for {}-{}", countryCode, indicatorId);
                return new WorldBankResponse(Optional.empty(), Collections.emptyList());
            }

            Optional<WorldBankMetadata> metadata = Optional.empty();
            List<WorldBankIndicatorData> data = Collections.emptyList();

            // Pierwszy element to metadane
            if (rawResponse.size() > 0 && rawResponse.get(0) instanceof Map<?, ?> metaMap) {
                metadata = Optional.of(objectMapper.convertValue(metaMap, WorldBankMetadata.class));
            }

            // Drugi element to lista danych
            if (rawResponse.size() > 1 && rawResponse.get(1) instanceof List<?> dataList) {
                data = objectMapper.convertValue(
                        dataList,
                        new TypeReference<List<WorldBankIndicatorData>>() {}
                );
            }

            // Obsługa przypadków braku danych
            if (metadata.isPresent() && metadata.get().total() != null && metadata.get().total() == 0 && data.isEmpty()) {
                log.info("No data found for indicator {} in country {}.", indicatorId, countryCode);
            }

            return new WorldBankResponse(metadata, data);

        } catch (HttpClientErrorException e) {
            String errorMessage = String.format("HTTP Error from World Bank API: %s - %s",
                    e.getStatusCode(), e.getResponseBodyAsString());
            log.error(errorMessage, e);
            throw new WorldBankApiException(errorMessage);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to call World Bank API or parse response: %s", e.getMessage());
            log.error(errorMessage, e);
            throw new WorldBankApiException(errorMessage);
        }
    }

}