package com.milosz.podsiadly.domain.simulation.data.service;

import com.milosz.podsiadly.domain.simulation.data.api.FinancialNewsApiClient;
import com.milosz.podsiadly.domain.simulation.data.dto.NewsApiArticle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialNewsService {

    private final FinancialNewsApiClient newsApiClient;
    private final List<NewsApiArticle> cachedNewsArticles = new CopyOnWriteArrayList<>(); // Thread-safe list

    /**
     * Fetches and caches the latest financial news.
     * @param query The search query for news (e.g., "banking crisis", "interest rate hike").
     * @param pageSize The number of articles to fetch per page.
     * @param page The page number.
     * @return A list of fetched news articles.
     */
    public List<NewsApiArticle> fetchAndCacheLatestNews(String query, int pageSize, int page) {
        log.info("Attempting to fetch latest financial news for query: {}", query);
        Optional<List<NewsApiArticle>> articlesOpt = newsApiClient.getFinancialNews(query, pageSize, page);

        if (articlesOpt.isPresent()) {
            List<NewsApiArticle> fetchedArticles = articlesOpt.get();
            cachedNewsArticles.clear(); // Clear old cache
            cachedNewsArticles.addAll(fetchedArticles);
            log.info("Successfully cached {} news articles for query: {}", fetchedArticles.size(), query);
            return Collections.unmodifiableList(cachedNewsArticles);
        } else {
            log.warn("Failed to fetch financial news for query: {}", query);
        }
        return Collections.unmodifiableList(cachedNewsArticles); // Return current (possibly empty/stale) cache
    }

    /**
     * Retrieves the latest news articles from cache. If cache is empty, attempts to fetch with a default query.
     * @return A list of cached news articles.
     */
    public List<NewsApiArticle> getLatestNews() {
        if (cachedNewsArticles.isEmpty()) {
            log.warn("News cache is empty. Attempting to fetch now with default query 'financial news'.");
            return fetchAndCacheLatestNews("financial news", 10, 1); // Default query
        }
        return Collections.unmodifiableList(cachedNewsArticles);
    }

    /**
     * Get recent news articles published after a specific time.
     * @param since A LocalDateTime to filter articles published after.
     * @return A list of relevant news articles.
     */
    public List<NewsApiArticle> getNewsSince(LocalDateTime since) {
        return cachedNewsArticles.stream()
                .filter(article -> article.publishedAt() != null && article.publishedAt().isAfter(since))
                .collect(Collectors.toUnmodifiableList());
    }
}