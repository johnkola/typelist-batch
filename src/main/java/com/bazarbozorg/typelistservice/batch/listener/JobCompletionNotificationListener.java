package com.bazarbozorg.typelistservice.batch.listener;

import com.bazarbozorg.typelistservice.batch.BatchUtils;
import com.bazarbozorg.typelistservice.batch.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Listener that handles job completion events and notifications.
 */
@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);
    
    private final NotificationService notificationService;
    private final BatchUtils batchUtils;

    /**
     * Creates a new JobCompletionNotificationListener with the specified dependencies.
     *
     * @param notificationService Service for sending notifications
     * @param batchUtils Batch utilities for file handling
     */
    @Autowired
    public JobCompletionNotificationListener(
            NotificationService notificationService,
            BatchUtils batchUtils) {
        this.notificationService = notificationService;
        this.batchUtils = batchUtils;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // Reset the skip counter before starting the job
        batchUtils.resetSkipCounter();
        
        log.info("Starting job: {} at {}", 
                jobExecution.getJobInstance().getJobName(), 
                LocalDateTime.now());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        
        // Properly convert Date to LocalDateTime
        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();
        
        long executionTime = 0;
        if (startTime != null && endTime != null) {
            executionTime = Duration.between(startTime, endTime).toMillis();
        }
        
        // For calculating skip rate
        int skipCount = batchUtils.getSkipCount();
        long readCount = jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getReadCount())
                .sum();
        
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("Job {} completed successfully! Execution time: {} ms", jobName, executionTime);
            log.info("Read count: {}, Skip count: {}, Skip rate: {}", 
                    readCount, skipCount, calculateSkipRate(skipCount, readCount));
            
            // Get the processed file from job parameters
            String filePath = jobExecution.getJobParameters().getString("filePath");
            File[] processedFiles = null;
            
            if (filePath != null) {
                processedFiles = new File[] { new File(filePath) };
                // Try to archive the processed file
                try {
                    batchUtils.archiveFile(new File(filePath));
                } catch (Exception e) {
                    log.error("Failed to archive file: {}", filePath, e);
                }
            }
            
            // Send notification for successful job completion
            notificationService.sendJobCompletionNotification(jobName, executionTime, processedFiles);
        } else {
            log.error("Job {} failed! Status: {}, Execution time: {} ms", 
                    jobName, jobExecution.getStatus(), executionTime);
            
            // Get the error message
            String errorMessage = jobExecution.getAllFailureExceptions().stream()
                    .map(Throwable::getMessage)
                    .findFirst()
                    .orElse("Unknown error");
            
            // Get the processed file from job parameters
            String filePath = jobExecution.getJobParameters().getString("filePath");
            File file = null;
            
            if (filePath != null) {
                file = new File(filePath);
                // Try to move the file to error directory
                try {
                    batchUtils.moveToErrorDirectory(file, new Exception(errorMessage));
                } catch (Exception e) {
                    log.error("Failed to move file to error directory: {}", filePath, e);
                }
            }
            
            // Send notification for job failure
            notificationService.sendJobErrorNotification(jobName, errorMessage, file);
        }
        
        // Send warning if there were many skipped items
        if (skipCount > 0) {
            String filePath = jobExecution.getJobParameters().getString("filePath");
            File file = filePath != null ? new File(filePath) : null;
            notificationService.sendSkippedItemsWarning(jobName, skipCount, file);
        }
    }
    

    /**
     * Calculates the skip rate as a percentage.
     *
     * @param skipCount Number of skipped items
     * @param readCount Number of read items
     * @return Skip rate as a percentage string
     */
    private String calculateSkipRate(int skipCount, long readCount) {
        if (readCount == 0) {
            return "0.00%";
        }
        double skipRate = (double) skipCount / (skipCount + readCount) * 100;
        return String.format("%.2f%%", skipRate);
    }
}