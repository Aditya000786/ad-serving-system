package com.adserving.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.adserving")
@EnableScheduling
@EnableKafka
public class AdServingApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdServingApplication.class, args);
    }
}
