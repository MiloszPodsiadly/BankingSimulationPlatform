package com.milosz.podsiadly.infrastructure.integration.exchangerates.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

// Record dla błędu zwracanego przez Fixer.io API
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FixerError(
        int code,
        String type,
        String info
) {}