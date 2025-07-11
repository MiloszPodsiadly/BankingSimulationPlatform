package com.milosz.podsiadly.infrastructure.integration.news;

import com.milosz.podsiadly.infrastructure.integration.news.dto.NewsApiResponse;

public interface NewsApiClient {

    /**
     * Pobiera artykuły spełniające określone kryteria, np. wyszukiwanie ogólne.
     * @param query Fraza wyszukiwania.
     * @param language Kod języka (np. "en", "pl").
     * @param pageSize Maksymalna liczba artykułów na stronę.
     * @param page Numer strony.
     * @return Odpowiedź API zawierająca listę artykułów.
     */

    NewsApiResponse getArticlesEverything(String query, String language, Integer pageSize, Integer page);

    /**
     * Pobiera najważniejsze nagłówki dla określonego kraju lub kategorii.
     * @param country Kod kraju (np. "us", "pl").
     * @param category Kategoria (np. "business", "technology").
     * @param pageSize Maksymalna liczba artykułów na stronę.
     * @param page Numer strony.
     * @return Odpowiedź API zawierająca listę artykułów.
     */
    NewsApiResponse getTopHeadlines(String country, String category, Integer pageSize, Integer page);
}