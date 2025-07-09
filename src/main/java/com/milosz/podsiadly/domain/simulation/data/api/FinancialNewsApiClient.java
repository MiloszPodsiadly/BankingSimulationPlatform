package com.milosz.podsiadly.domain.simulation.data.api;

import com.milosz.podsiadly.domain.simulation.data.dto.NewsApiArticle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class FinancialNewsApiClient {

    private final RestTemplate restTemplate;

    @Value("${api.news.base-url}")
    private String baseUrl;

    @Value("${api.news.api-key}")
    private String apiKey;

    public FinancialNewsApiClient(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    public Optional<List<NewsApiArticle>> getFinancialNews(String query, int pageSize, int page) {
        // NewsAPI requires `from` and `to` dates in specific format (YYYY-MM-DDTHH:MM:SS)
        // For simplicity, let's fetch recent news without specific date range for now.
        // Or fetch for a specific recent period.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusDays(1);

        String from = twentyFourHoursAgo.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String to = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);


        String url = String.format("%s?q=%s&apiKey=%s&language=en&sortBy=publishedAt&pageSize=%d&page=%d&from=%s&to=%s",
                baseUrl, query, apiKey, pageSize, page, from, to);
        log.info("Fetching financial news from: {}", url);

        try {
            // NewsAPI returns a JSON object with "articles" field which is a List<NewsApiArticle>
            // We need to unwrap it. A simple approach is to use Map and extract.
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = restTemplate.getForObject(url, Map.class);

            if (responseMap != null && "ok".equals(responseMap.get("status"))) {
                List<Map<String, Object>> articlesData = (List<Map<String, Object>>) responseMap.get("articles");
                List<NewsApiArticle> articles = articlesData.stream()
                        .map(this::mapToNewsApiArticle) // Custom mapping logic
                        .toList();
                log.info("Successfully fetched {} news articles for query: {}", articles.size(), query);
                return Optional.of(articles);
            } else if (responseMap != null && responseMap.containsKey("message")) {
                log.error("Error fetching financial news from NewsAPI: {}", responseMap.get("message"));
                return Optional.empty();
            } else {
                log.warn("NewsAPI response was null or indicated failure.");
                return Optional.empty();
            }
        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching financial news: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error fetching financial news: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    // Helper method to manually map the raw Map<String, Object> to NewsApiArticle record
    private NewsApiArticle mapToNewsApiArticle(Map<String, Object> articleMap) {
        String publishedAtStr = (String) articleMap.get("publishedAt");
        LocalDateTime publishedAt = null;
        if (publishedAtStr != null) {
            try {
                // NewsAPI returns ISO 8601 format, LocalDateTime.parse handles it
                publishedAt = LocalDateTime.parse(publishedAtStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (Exception e) {
                log.warn("Could not parse publishedAt date: {}", publishedAtStr, e);
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, String> sourceMap = (Map<String, String>) articleMap.get("source");
        NewsApiArticle.Source source = null;
        if (sourceMap != null) {
            source = new NewsApiArticle.Source(sourceMap.get("id"), sourceMap.get("name"));
        }

        return new NewsApiArticle(
                source,
                (String) articleMap.get("author"),
                (String) articleMap.get("title"),
                (String) articleMap.get("description"),
                (String) articleMap.get("url"),
                (String) articleMap.get("urlToImage"),
                publishedAt,
                (String) articleMap.get("content")
        );
    }
}