package com.bazarbozorg.typelistservice.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;

/**
 * Custom skip policy implementation
 */
public class FileVerificationSkipper implements SkipPolicy {
    private static final Logger log = LoggerFactory.getLogger(FileVerificationSkipper.class);


    @Override
    public boolean shouldSkip(Throwable exception, long skipCount) throws SkipLimitExceededException {
        // Log the error
        log.error("Error processing record: {}", exception.getMessage());

        // You can implement conditional skipping based on exception type
        // For now, we'll skip all errors to avoid job failure
        return true;
    }
}