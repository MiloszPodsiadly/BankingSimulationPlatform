package com.milosz.podsiadly.infrastructure.integration.news.dto;

import java.time.OffsetDateTime; // Zalecane do parsowania daty z UTC/Z

// Rekord reprezentujący pojedynczy artykuł
public record Article(
        Source source,
        String author,
        String title,
        String description,
        String url,
        String urlToImage,
        OffsetDateTime publishedAt, // Używamy OffsetDateTime do daty z ISO 8601
        String content
) {}