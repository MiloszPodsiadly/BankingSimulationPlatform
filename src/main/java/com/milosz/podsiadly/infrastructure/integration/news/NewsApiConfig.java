package com.milosz.podsiadly.infrastructure.integration.news;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Configuration
public class NewsApiConfig {

    // Możemy zdefiniować osobny bean RestTemplate dla News API,
    // aby mieć niezależne timeouty lub interceptory.
    // Domyślnie Spring wstrzyknie główny RestTemplate bean, jeśli nie ma innego z kwalifikatorem.
    // Tutaj dla przykładu, użyjemy tego samego schematu co dla FixerIoConfig, tworząc osobny bean.
    @Bean("newsApiRestTemplate") // Nadajemy nazwę beanowi, żeby uniknąć konfliktów i wstrzykiwać konkretny
    public RestTemplate newsApiRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(8)) // Może trochę dłuższe timeouty niż dla Fixer.io
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
    }

    // Ponieważ NewsApiClientImpl jest oznaczony jako @Service, Spring sam go wykryje.
    // Musimy jednak upewnić się, że wstrzykiwana jest prawidłowa instancja RestTemplate.
    // Możesz użyć @Qualifier("newsApiRestTemplate") w konstruktorze NewsApiClientImpl,
    // jeśli masz wiele RestTemplate beanów. Dla uproszczenia, jeśli tylko ten jeden jest nazwany,
    // a reszta nienazwana, Spring poradzi sobie sam. Ale jawne jest zawsze lepsze.
    // Zaktualizujmy konstruktor NewsApiClientImpl, aby przyjmował @Qualifier.
}