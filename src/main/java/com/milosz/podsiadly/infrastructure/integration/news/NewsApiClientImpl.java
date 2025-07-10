package com.milosz.podsiadly.infrastructure.integration.news;

import com.milosz.podsiadly.infrastructure.integration.news.dto.NewsApiResponse;
import com.milosz.podsiadly.common.exception.NewsApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Service
@Slf4j
public class NewsApiClientImpl implements NewsApiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public NewsApiClientImpl(
            RestTemplate restTemplate,
            @Value("${api.news.base-url}") String baseUrl,
            @Value("${api.news.api-key}") String apiKey
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    private NewsApiResponse executeApiCall(UriComponentsBuilder uriBuilder, String endpointName) {
        uriBuilder.queryParam("apiKey", apiKey);
        String url = uriBuilder.toUriString();
        log.info("Calling News API ({}) endpoint: {}", endpointName, url);

        try {
            NewsApiResponse response = restTemplate.getForObject(url, NewsApiResponse.class);

            // News API w przypadku błędu zwraca status "error" i pola 'code', 'message'
            if (response != null && "error".equalsIgnoreCase(response.status())) {
                String errorMessage = String.format("News API Error [Code: %s, Message: %s]",
                        Optional.ofNullable(response.code()).orElse("N/A"),
                        Optional.ofNullable(response.message()).orElse("Unknown error message"));
                log.error(errorMessage);
                throw new NewsApiException(errorMessage);
            }

            if (response == null) {
                String message = "News API returned null response.";
                log.error(message);
                throw new NewsApiException(message);
            }

            return response;

        } catch (HttpClientErrorException e) {
            String errorMessage = String.format("HTTP Error from News API: %s - %s", e.getStatusCode(), e.getResponseBodyAsString());
            log.error(errorMessage, e);
            throw new NewsApiException(errorMessage);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to call News API or parse response: %s", e.getMessage());
            log.error(errorMessage, e);
            throw new NewsApiException(errorMessage);
        }
    }
    /*
    @Override
    public NewsApiResponse getArticlesEverything(String query, String language, Integer pageSize, Integer page) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/everything"); // 🔧 poprawka

        if (query != null && !query.isBlank()) {
            uriBuilder.queryParam("q", query);
        }
        if (language != null && !language.isBlank()) {
            uriBuilder.queryParam("language", language);
        }
        if (pageSize != null && pageSize > 0) {
            uriBuilder.queryParam("pageSize", pageSize);
        }
        if (page != null && page > 0) {
            uriBuilder.queryParam("page", page);
        }

        return executeApiCall(uriBuilder, "everything");
    }
*/
    @Override
    public NewsApiResponse getTopHeadlines(String country, String category, Integer pageSize, Integer page) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/top-headlines"); // 🔧 poprawka

        if (country != null && !country.isBlank()) {
            uriBuilder.queryParam("country", country);
        }
        if (category != null && !category.isBlank()) {
            uriBuilder.queryParam("category", category);
        }
        if (pageSize != null && pageSize > 0) {
            uriBuilder.queryParam("pageSize", pageSize);
        }
        if (page != null && page > 0) {
            uriBuilder.queryParam("page", page);
        }

        return executeApiCall(uriBuilder, "top-headlines");
    }
}