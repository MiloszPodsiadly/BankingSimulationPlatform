package com.milosz.podsiadly.infrastructure.integration.news.dto;

import java.util.List;

// Główny rekord odpowiedzi z News API
public record NewsApiResponse(
        String status,         // "ok" or "error"
        Integer totalResults,  // Totalna liczba wyników
        List<Article> articles, // Lista artykułów
        String code,           // Kod błędu (gdy status="error")
        String message         // Wiadomość o błędzie (gdy status="error")
) {}