package com.milosz.podsiadly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@EnableScheduling
@EnableConfigurationProperties
@SpringBootApplication
@EntityScan("com.milosz.podsiadly")
@EnableJpaRepositories("com.milosz.podsiadly")
public class BankingSimulationPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingSimulationPlatformApplication.class, args);
    }

}
