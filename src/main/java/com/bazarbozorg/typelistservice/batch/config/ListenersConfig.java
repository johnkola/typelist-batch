package com.bazarbozorg.typelistservice.batch.config;

import com.bazarbozorg.typelistservice.batch.BatchUtils;
import com.bazarbozorg.typelistservice.batch.listener.JobCompletionNotificationListener;
import com.bazarbozorg.typelistservice.batch.listener.PerformanceMonitoringListener;
import com.bazarbozorg.typelistservice.batch.service.NotificationService;
import com.bazarbozorg.typelistservice.batch.util.MemoryMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for batch job listeners.
 */
@Configuration
public class ListenersConfig {

    private final BatchUtils batchUtils;
    private final NotificationService notificationService;
    private final MemoryMonitor memoryMonitor;

    /**
     * Creates a new ListenersConfig with the specified dependencies.
     *
     * @param batchUtils Batch utilities for file handling
     * @param notificationService Service for sending notifications
     * @param memoryMonitor Memory monitoring utility
     */
    @Autowired
    public ListenersConfig(
            BatchUtils batchUtils, 
            NotificationService notificationService,
            MemoryMonitor memoryMonitor) {
        this.batchUtils = batchUtils;
        this.notificationService = notificationService;
        this.memoryMonitor = memoryMonitor;
    }

    /**
     * Creates a JobCompletionNotificationListener bean.
     *
     * @return The configured listener
     */
    @Bean
    public JobCompletionNotificationListener jobCompletionNotificationListener() {
        return new JobCompletionNotificationListener(notificationService, batchUtils);
    }

    /**
     * Creates a PerformanceMonitoringListener bean.
     *
     * @return The configured listener
     */
    @Bean
    public PerformanceMonitoringListener performanceMonitoringListener() {
        return new PerformanceMonitoringListener(memoryMonitor);
    }
}