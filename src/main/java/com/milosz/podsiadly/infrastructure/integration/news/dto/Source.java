package com.milosz.podsiadly.infrastructure.integration.news.dto;

// Rekord reprezentujący źródło artykułu
public record Source(
        String id,
        String name
) {}