package com.milosz.podsiadly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@EnableScheduling
@EnableConfigurationProperties
@SpringBootApplication
public class BankingSimulationPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingSimulationPlatformApplication.class, args);
    }

}
