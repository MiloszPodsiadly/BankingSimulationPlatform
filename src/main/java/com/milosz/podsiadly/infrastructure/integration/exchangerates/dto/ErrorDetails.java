package com.milosz.podsiadly.infrastructure.integration.exchangerates.dto;

// UPEWNIJ SIĘ, ŻE TO JEST 'public record', A NIE 'public class'
public record ErrorDetails(
        Integer code,
        String type,
        String info
) {}