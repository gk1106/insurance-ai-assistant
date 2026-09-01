package com.insuranceai.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class InsuranceAiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsuranceAiBackendApplication.class, args);
    }
}
