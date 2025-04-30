package com.bazarbozorg.typelistservice.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application class for the TypeList batch processing service.
 * This service reads TypeList, TypeItem, and TypeSet data from files and updates
 * the TypeList web service.
 */

@SpringBootApplication
@EnableBatchProcessing
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BatchApplication.class);
        app.setAdditionalProfiles("sequential");
        app.run(args);
    }
}