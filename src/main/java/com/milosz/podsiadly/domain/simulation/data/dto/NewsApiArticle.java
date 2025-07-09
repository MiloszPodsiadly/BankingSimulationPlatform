package com.milosz.podsiadly.domain.simulation.data.dto;

import java.time.LocalDateTime;

public record NewsApiArticle(
        Source source,
        String author,
        String title,
        String description,
        String url,
        String urlToImage,
        LocalDateTime publishedAt,
        String content
) {
    // Zagnieżdżony rekord dla Source
    public record Source(
            String id,
            String name
    ) {}
}