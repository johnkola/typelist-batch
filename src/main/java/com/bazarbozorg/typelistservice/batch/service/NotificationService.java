package com.bazarbozorg.typelistservice.batch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;

/**
 * Service for sending email notifications about batch job events.
 */
@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.recipients:admin@example.com}")
    private String recipients;

    @Value("${notification.email.sender:typelistservice@example.com}")
    private String sender;

    private final JavaMailSender mailSender;

    /**
     * Creates a new NotificationService.
     *
     * @param mailSender The JavaMailSender to use for sending emails
     */
    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends a notification for successful job completion.
     *
     * @param jobName The name of the job that completed
     * @param executionTime The execution time in milliseconds
     * @param processedFiles The files that were processed
     */
    public void sendJobCompletionNotification(String jobName, long executionTime, File[] processedFiles) {
        if (!emailEnabled) {
            log.debug("Email notifications are disabled. Skipping job completion notification.");
            return;
        }

        String subject = "Job Completed: " + jobName;
        StringBuilder message = new StringBuilder("Job has completed successfully.\n\n");
        message.append("Job: ").append(jobName).append("\n");
        message.append("Execution Time: ").append(formatExecutionTime(executionTime)).append("\n");
        
        if (processedFiles != null && processedFiles.length > 0) {
            message.append("Processed Files:\n");
            Arrays.stream(processedFiles).forEach(file -> 
                message.append("- ").append(file.getName()).append("\n")
            );
        }
        
        sendEmail(subject, message.toString());
    }

    /**
     * Sends a notification for job error.
     *
     * @param jobName The name of the job
     * @param errorMessage The error message
     * @param file The file that caused the error, if applicable
     */
    public void sendJobErrorNotification(String jobName, String errorMessage, File file) {
        if (!emailEnabled) {
            log.debug("Email notifications are disabled. Skipping job error notification.");
            return;
        }

        String subject = "Job Error: " + jobName;
        StringBuilder message = new StringBuilder("An error occurred during job execution.\n\n");
        message.append("Job: ").append(jobName).append("\n");
        message.append("Error: ").append(errorMessage).append("\n");
        
        if (file != null) {
            message.append("File: ").append(file.getName()).append("\n");
        }
        
        sendEmail(subject, message.toString());
    }

    /**
     * Sends a notification for large number of skipped items.
     *
     * @param jobName The name of the job
     * @param skipCount The number of skipped items
     * @param file The file being processed
     */
    public void sendSkippedItemsWarning(String jobName, int skipCount, File file) {
        if (!emailEnabled || skipCount < 100) { // Only send if significant number of skips
            return;
        }

        String subject = "Warning: High Skip Count in " + jobName;
        StringBuilder message = new StringBuilder("A high number of items were skipped during processing.\n\n");
        message.append("Job: ").append(jobName).append("\n");
        message.append("Skip Count: ").append(skipCount).append("\n");
        
        if (file != null) {
            message.append("File: ").append(file.getName()).append("\n");
        }
        
        message.append("\nThis may indicate data quality issues in the input file.");
        
        sendEmail(subject, message.toString());
    }

    /**
     * Helper method to send an email.
     *
     * @param subject The email subject
     * @param body The email body
     */
    private void sendEmail(String subject, String body) {
        if (!emailEnabled) {
            log.debug("Email would be sent: Subject=\"{}\"", subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(recipients.split(","));
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            log.debug("Sent email notification: {}", subject);
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Formats execution time in a human-readable format.
     *
     * @param executionTimeMs Execution time in milliseconds
     * @return Formatted execution time
     */
    private String formatExecutionTime(long executionTimeMs) {
        long seconds = executionTimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds %= 60;
        minutes %= 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}