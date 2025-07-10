package com.milosz.podsiadly.infrastructure.integration.worldbank;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.milosz.podsiadly.infrastructure.integration.worldbank.dto.WorldBankIndicatorData;
import com.milosz.podsiadly.infrastructure.integration.worldbank.dto.WorldBankMetadata;
import com.milosz.podsiadly.common.exception.WorldBankApiException;
import com.milosz.podsiadly.infrastructure.integration.worldbank.dto.WorldBankResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
    private final ObjectMapper objectMapper;

    public WorldBankClientImpl(
            @Qualifier("worldBankRestTemplate") RestTemplate restTemplate,
            @Value("${api.worldbank.base-url}") String baseUrl,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
    }
    @PostConstruct
    public void logStartupInfo() {
        log.info("Active profile: {}", System.getProperty("spring.profiles.active"));
        log.info("World Bank Base URL: {}", baseUrl);
    }

    @Override
    public WorldBankResponse getIndicatorData(String countryCode, String indicatorId, String dateFrom, String dateTo) {
        if (countryCode == null || countryCode.isBlank() || indicatorId == null || indicatorId.isBlank()) {
            throw new IllegalArgumentException("Country code and indicator ID cannot be null or empty.");
        }

        // Budowanie URI z parametrami
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(baseUrl + "/country/{countryCode}/indicator/{indicatorId}")
                .queryParam("format", "json");

        if (dateFrom != null && !dateFrom.isBlank() && dateTo != null && !dateTo.isBlank()) {
            uriBuilder.queryParam("date", dateFrom + ":" + dateTo);
        } else if (dateFrom != null && !dateFrom.isBlank()) {
            uriBuilder.queryParam("date", dateFrom);
        } else if (dateTo != null && !dateTo.isBlank()) {
            uriBuilder.queryParam("date", dateTo);
        }

        String url = uriBuilder
                .buildAndExpand(countryCode, indicatorId)
                .toUriString();

        log.info("Calling World Bank API: {}", url);

        String rawJson = null;

        try {
            ResponseEntity<String> rawResponseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    String.class
            );

            rawJson = rawResponseEntity.getBody();
            log.info("Raw World Bank API response for {}-{}: {}", countryCode, indicatorId, rawJson);

            if (rawJson == null || rawJson.isBlank()) {
                log.warn("World Bank API returned empty or null raw JSON response for {}-{}", countryCode, indicatorId);
                return new WorldBankResponse(Optional.empty(), Collections.emptyList());
            }

            JsonNode jsonNode = objectMapper.readTree(rawJson);
            if (!jsonNode.isArray()) {
                log.error("Unexpected JSON structure (not an array) from World Bank API for {}-{}: {}", countryCode, indicatorId, rawJson);
                throw new WorldBankApiException("Unexpected JSON structure (not array). Possibly an API error: " + rawJson);
            }

            // Parsowanie na List<List<Object>>
            List<List<Object>> parsedResponse = objectMapper.convertValue(jsonNode, new TypeReference<>() {});

            Optional<WorldBankMetadata> metadata = Optional.empty();
            List<WorldBankIndicatorData> data = Collections.emptyList();

            if (parsedResponse.size() > 0 && parsedResponse.get(0) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metaMap = (Map<String, Object>) parsedResponse.get(0);
                metadata = Optional.of(objectMapper.convertValue(metaMap, WorldBankMetadata.class));
            }

            if (parsedResponse.size() > 1 && parsedResponse.get(1) instanceof List) {
                List<?> rawDataList = (List<?>) parsedResponse.get(1);

                List<Map<String, Object>> dataList = rawDataList.stream()
                        .filter(item -> item instanceof Map)
                        .map(item -> (Map<String, Object>) item)
                        .toList();

                data = objectMapper.convertValue(dataList, new TypeReference<List<WorldBankIndicatorData>>() {});
            }

            if (metadata.isPresent() && metadata.get().total() != null && metadata.get().total() == 0 && data.isEmpty()) {
                log.info("No data found for indicator {} in country {}.", indicatorId, countryCode);
            }

            return new WorldBankResponse(metadata, data);

        } catch (HttpClientErrorException e) {
            String errorMessage = String.format("HTTP Error from World Bank API: %s - %s",
                    e.getStatusCode(), e.getResponseBodyAsString());
            log.error(errorMessage, e);
            throw new WorldBankApiException(errorMessage);
        } catch (JsonProcessingException e) {
            String errorMessage = String.format("Failed to parse JSON response from World Bank API for %s-%s: %s. Raw JSON: %s",
                    countryCode, indicatorId, e.getMessage(), (rawJson != null ? rawJson : "N/A"));
            log.error(errorMessage, e);
            throw new WorldBankApiException(errorMessage);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to call World Bank API or process response for %s-%s: %s",
                    countryCode, indicatorId, e.getMessage());
            log.error(errorMessage, e);
            throw new WorldBankApiException(errorMessage);
        }
    }
}
