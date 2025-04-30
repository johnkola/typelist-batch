package com.bazarbozorg.typelistservice.batch.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for API retry capabilities, designed to work with the 
 * ApiClientConfiguration from the service client jar.
 */
@Configuration
public class RetryConfiguration {
    private static final Logger log = LoggerFactory.getLogger(RetryConfiguration.class);

    @Value("${api.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${api.retry.backoff-ms:1000}")
    private long backoffPeriodMs;

    /**
     * Creates a retry template for API calls.
     *
     * @return The configured RetryTemplate
     */
    @Bean(name = "apiRetryTemplate")
    public RetryTemplate apiRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Configure retry policy with specific exceptions to retry on
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(ResourceAccessException.class, true); // Connection issues
        retryableExceptions.put(RestClientException.class, true);     // General REST issues
        
        RetryPolicy retryPolicy = new SimpleRetryPolicy(maxRetryAttempts, retryableExceptions, true);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Configure backoff policy
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(backoffPeriodMs);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        log.info("Configured API retry with max attempts: {}, backoff period: {} ms", 
                maxRetryAttempts, backoffPeriodMs);
        
        return retryTemplate;
    }
}