package com.milosz.podsiadly.domain.simulation.data.service;

// --- ZMIANA 1: Poprawne importy DTO i interfejsu klienta API ---
import com.milosz.podsiadly.infrastructure.integration.news.NewsApiClient; // Wstrzykujemy interfejs klienta
import com.milosz.podsiadly.infrastructure.integration.news.dto.NewsApiResponse; // Potrzebujemy dostępu do statusu i listy artykułów
import com.milosz.podsiadly.infrastructure.integration.news.dto.Article; // NewsApiArticle zmienione na Article, zgodnie z Twoim DTO
// Import wyjątku, który rzuca NewsApiClientImpl
import com.milosz.podsiadly.common.exception.NewsApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime; // Dodany import dla OffsetDateTime
import java.util.Collections;
import java.util.List;
import java.util.Optional; // Nadal używamy Optional w niektórych miejscach dla semantyki
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialNewsService {

    // --- ZMIANA 2: Pole klienta API ---
    // Wstrzykujemy NewsApiClient (implementowany przez NewsApiClientImpl)
    private final NewsApiClient newsApiClient;

    // --- ZMIANA 3: Typ listy cache'owanej - teraz Articles ---
    private final List<Article> cachedNewsArticles = new CopyOnWriteArrayList<>(); // Thread-safe list

    /**
     * Fetches and caches the latest financial news.
     * @param query The search query for news (e.g., "banking crisis", "interest rate hike").
     * @param pageSize The number of articles to fetch per page.
     * @param page The page number.
     * @return A list of fetched news articles. Returns an unmodifiable empty list if fetching fails.
     */
    public List<Article> fetchAndCacheLatestNews(String query, int pageSize, int page) {
        log.info("Attempting to fetch latest financial news for query: {}", query);
        List<Article> fetchedArticles = Collections.emptyList(); // Domyślnie pusta lista

        try {
            // --- ZMIANA 4: Wywołanie odpowiedniej metody z NewsApiClient ---
            // newsApiClient.getArticlesEverything zwraca NewsApiResponse
            NewsApiResponse response = newsApiClient.getArticlesEverything(query, "en", pageSize, page);

            // --- ZMIANA 5: Ulepszona logika przetwarzania odpowiedzi NewsAPI ---
            if (response != null && "ok".equalsIgnoreCase(response.status()) && response.articles() != null) {
                fetchedArticles = response.articles();
                cachedNewsArticles.clear(); // Clear old cache
                cachedNewsArticles.addAll(fetchedArticles);
                log.info("Successfully cached {} news articles for query: {}", fetchedArticles.size(), query);
            } else {
                // Ten blok powinien być rzadko używany, jeśli NewsApiClientImpl poprawnie rzuca wyjątki
                // w przypadku status="error" lub braku artykułów.
                log.warn("News API response was successful (status 'ok') but contained no articles for query: {}. Cache remains unchanged.", query);
            }
        } catch (NewsApiException e) {
            // --- ZMIANA 6: Obsługa wyjątku rzuconego przez NewsApiClientImpl ---
            log.error("Failed to fetch financial news due to News API error: {}. Cache remains unchanged.", e.getMessage());
            // W tym przypadku cache pozostaje niezmieniony, co pozwala na użycie starych danych, jeśli były
        } catch (Exception e) {
            // --- ZMIANA 7: Obsługa innych nieoczekiwanych wyjątków ---
            log.error("An unexpected error occurred while fetching financial news for query {}: {}. Cache remains unchanged.", query, e.getMessage(), e);
        }
        return Collections.unmodifiableList(cachedNewsArticles); // Zawsze zwracamy niezmodyfikowaną mapę (bieżący cache)
    }

    /**
     * Retrieves the latest news articles from cache. If cache is empty, attempts to fetch with a default query.
     * @return A list of cached news articles.
     */
    public List<Article> getLatestNews() {
        if (cachedNewsArticles.isEmpty()) {
            log.warn("News cache is empty. Attempting to fetch now with default query 'financial news'.");
            return fetchAndCacheLatestNews("financial news", 10, 1); // Default query
        }
        return Collections.unmodifiableList(cachedNewsArticles);
    }

    /**
     * Get recent news articles published after a specific time.
     * Używa OffsetDateTime, co jest zgodne z Twoim DTO Article.
     * @param since An OffsetDateTime to filter articles published after.
     * @return A list of relevant news articles.
     */
    public List<Article> getNewsSince(OffsetDateTime since) { // Zmieniono typ parametru na OffsetDateTime
        return cachedNewsArticles.stream()
                .filter(article -> article.publishedAt() != null && article.publishedAt().isAfter(since))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Fetches and caches top financial headlines.
     * @param country The country to get headlines for (e.g., "us").
     * @param category The category (e.g., "business").
     * @param pageSize The number of articles to return per page.
     * @param page The page number.
     * @return A list of fetched news articles. Returns an unmodifiable empty list if fetching fails.
     */
    public List<Article> fetchAndCacheTopHeadlines(String country, String category, int pageSize, int page) {
        log.info("Attempting to fetch top headlines for country: {} and category: {}", country, category);
        List<Article> fetchedArticles = Collections.emptyList();

        try {
            // --- ZMIANA 8: Wywołanie odpowiedniej metody z NewsApiClient ---
            NewsApiResponse response = newsApiClient.getTopHeadlines(country, category, pageSize, page);

            if (response != null && "ok".equalsIgnoreCase(response.status()) && response.articles() != null) {
                fetchedArticles = response.articles();
                // Możesz zdecydować, czy chcesz mieszać cache czy używać osobnego dla top headlines
                // Na razie ta metoda zwraca tylko świeżo pobrane nagłówki, nie modyfikuje głównego cache.
                // Jeśli chcesz nadpisywać, dodaj cachedNewsArticles.clear();
                // cachedNewsArticles.addAll(fetchedArticles);
                log.info("Successfully fetched {} top headlines for country: {}.", fetchedArticles.size(), country);
            } else {
                log.warn("News API response for top headlines was successful (status 'ok') but contained no articles for country: {}. No cache update.", country);
            }
        } catch (NewsApiException e) {
            log.error("Failed to fetch top headlines due to News API error: {}. No cache update.", e.getMessage());
        } catch (Exception e) {
            log.error("An unexpected error occurred while fetching top headlines for country {}: {}. No cache update.", country, e.getMessage(), e);
        }
        return Collections.unmodifiableList(fetchedArticles); // Zwraca tylko świeżo pobrane nagłówki
    }
}