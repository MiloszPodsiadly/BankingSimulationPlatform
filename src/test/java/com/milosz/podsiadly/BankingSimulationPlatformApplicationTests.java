package com.milosz.podsiadly;

import com.milosz.podsiadly.core.kafka.consumer.KafkaConsumerService;
import com.milosz.podsiadly.core.kafka.producer.KafkaProducerService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BankingSimulationPlatformApplicationTests {

    @MockBean
    KafkaConsumerService kafkaConsumerService;

    @MockBean
    KafkaProducerService kafkaProducerService;

    @Test
    void contextLoads() {
    }
}
