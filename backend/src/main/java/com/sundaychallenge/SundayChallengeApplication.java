package com.sundaychallenge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Sunday Challenge - Main Spring Boot Application Entry Point.
 */
@SpringBootApplication
@EntityScan("com.sundaychallenge.entity")
@EnableJpaRepositories("com.sundaychallenge.repository")
public class SundayChallengeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SundayChallengeApplication.class, args);
    }
}
