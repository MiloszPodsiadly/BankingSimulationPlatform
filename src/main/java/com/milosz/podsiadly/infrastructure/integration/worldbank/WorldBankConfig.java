package com.milosz.podsiadly.infrastructure.integration.worldbank;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class WorldBankConfig {

    @Bean("worldBankRestTemplate") // Nazwa dla RestTemplate, aby można było ją wstrzyknąć konkretnie
    public RestTemplate worldBankRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10)) // Dłuższe timeouty, jeśli API jest wolniejsze
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
    }
}