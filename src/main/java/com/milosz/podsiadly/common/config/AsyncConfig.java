package com.milosz.podsiadly.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // Włącza wsparcie dla adnotacji @Async
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);       // Minimalna liczba wątków
        executor.setMaxPoolSize(10);       // Maksymalna liczba wątków
        executor.setQueueCapacity(25);     // Pojemność kolejki zadań
        executor.setThreadNamePrefix("AsyncTaskExecutor-"); // Prefiks nazwy wątków
        executor.initialize();
        return executor;
    }
}
