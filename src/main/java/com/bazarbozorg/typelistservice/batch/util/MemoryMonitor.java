package com.bazarbozorg.typelistservice.batch.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Utility class for monitoring and logging JVM memory usage.
 * This helps track memory consumption during batch processing.
 */
@Component
public class MemoryMonitor {
    private static final Logger log = LoggerFactory.getLogger(MemoryMonitor.class);
    
    // Conversion factor for bytes to MB
    private static final double MB_CONVERSION = 1024 * 1024.0;
    
    /**
     * Logs the current memory usage with a provided context message
     * 
     * @param context Context information for the log entry
     */
    public void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        
        // Calculate memory usage
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        
        // Convert to MB for more readable output
        double totalMemoryMB = totalMemory / MB_CONVERSION;
        double freeMemoryMB = freeMemory / MB_CONVERSION;
        double usedMemoryMB = usedMemory / MB_CONVERSION;
        double maxMemoryMB = maxMemory / MB_CONVERSION;
        double usedPercentage = (usedMemory * 100.0) / maxMemory;
        
        // Log memory usage
        log.info("Memory usage [{}]: Used: {:.2f}MB ({:.2f}%), Free: {:.2f}MB, Total: {:.2f}MB, Max: {:.2f}MB",
                context, usedMemoryMB, usedPercentage, freeMemoryMB, totalMemoryMB, maxMemoryMB);
        
        // Add a warning if memory usage is high (above 80%)
        if (usedPercentage > 80.0) {
            log.warn("High memory usage detected [{}]: {:.2f}% of maximum memory", context, usedPercentage);
        }
    }
    
    /**
     * Requests garbage collection and logs memory usage before and after.
     * Note: This is only a request to the JVM, which may not actually perform GC.
     * 
     * @param context Context information for the log entry
     */
    public void requestGCAndLogMemory(String context) {
        logMemoryUsage(context + " (Before GC request)");
        
        // Request garbage collection
        System.gc();
        
        logMemoryUsage(context + " (After GC request)");
    }
}