package com.milosz.podsiadly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@EnableScheduling
@SpringBootApplication
public class BankingSimulationPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankingSimulationPlatformApplication.class, args);
    }

}
