package com.bazarbozorg.typelistservice.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wrapper for ItemWriter that tracks progress metrics such as items processed and processing rate.
 * Useful for monitoring long-running batch jobs.
 *
 * @param <T> Type of items being written
 */
public class ProgressTrackingWriter<T> implements ItemWriter<T> {
    private static final Logger log = LoggerFactory.getLogger(ProgressTrackingWriter.class);
    
    // Delegate writer that will do the actual writing
    private final ItemWriter<T> delegate;
    
    // Progress tracking counters
    private final AtomicLong itemsProcessed = new AtomicLong(0);
    private final AtomicLong totalChunks = new AtomicLong(0);
    
    // Timing variables
    private LocalDateTime startTime;
    private LocalDateTime lastLogTime;
    
    // Log progress every N chunks
    private static final int LOG_FREQUENCY_CHUNKS = 10;
    
    /**
     * Creates a new progress tracking writer wrapping the specified delegate writer.
     *
     * @param delegate The writer to delegate actual writing to
     */
    public ProgressTrackingWriter(ItemWriter<T> delegate) {
        this.delegate = delegate;
        this.startTime = LocalDateTime.now();
        this.lastLogTime = startTime;
    }
    
    @Override
    public void write(Chunk<? extends T> items) throws Exception {
        // First delegate to the actual writer
        delegate.write(items);
        
        // Update counters
        long currentCount = itemsProcessed.addAndGet(items.size());
        long chunkNumber = totalChunks.incrementAndGet();
        
        // Log progress at regular intervals
        if (chunkNumber % LOG_FREQUENCY_CHUNKS == 0) {
            logProgress(currentCount, chunkNumber, items.size());
        }
    }
    
    /**
     * Logs current progress information including processing rate.
     *
     * @param currentCount Total items processed so far
     * @param chunkNumber Current chunk number
     * @param chunkSize Size of the current chunk
     */
    private void logProgress(long currentCount, long chunkNumber, int chunkSize) {
        LocalDateTime now = LocalDateTime.now();
        
        // Calculate total duration
        Duration totalDuration = Duration.between(startTime, now);
        long totalSeconds = totalDuration.getSeconds();
        
        // Calculate recent processing rate
        Duration sinceLastLog = Duration.between(lastLogTime, now);
        long recentSeconds = sinceLastLog.getSeconds();
        double recentItemsPerSecond = recentSeconds > 0 ? 
                (chunkSize * LOG_FREQUENCY_CHUNKS) / (double) recentSeconds : 0;
        
        // Calculate overall processing rate
        double overallItemsPerSecond = totalSeconds > 0 ? 
                currentCount / (double) totalSeconds : 0;
        
        // Estimate time remaining (based on overall rate)
        // This is a simple estimate and could be improved
        long remainingItems = estimateRemainingItems(currentCount);
        long estimatedSecondsRemaining = overallItemsPerSecond > 0 ? 
                (long)(remainingItems / overallItemsPerSecond) : -1;
        
        // Log progress information
        log.info("Progress: {} items processed in {} chunks ({} items/sec), recent rate: {:.2f} items/sec",
                currentCount, chunkNumber, String.format("%.2f", overallItemsPerSecond), recentItemsPerSecond);
        
        if (estimatedSecondsRemaining > 0) {
            long hours = estimatedSecondsRemaining / 3600;
            long minutes = (estimatedSecondsRemaining % 3600) / 60;
            long seconds = estimatedSecondsRemaining % 60;
            log.info("Estimated time remaining: {}h {}m {}s", hours, minutes, seconds);
        }
        
        // Update last log time
        lastLogTime = now;
    }
    
    /**
     * Estimates the number of items remaining to be processed.
     * This is a placeholder implementation and should be customized based on your specific needs.
     * 
     * @param currentCount Current number of items processed
     * @return Estimated number of items remaining
     */
    private long estimateRemainingItems(long currentCount) {
        // This is a placeholder. In a real implementation, you might:
        // 1. Get the total from the reader if it supports it
        // 2. Use a job parameter with the expected total
        // 3. Use a predefined estimate
        
        // Example: if you know the total items to process beforehand
        long estimatedTotal = -1; // Replace with actual total if known
        
        if (estimatedTotal > 0) {
            return Math.max(0, estimatedTotal - currentCount);
        }
        
        // If total is unknown, return -1
        return -1;
    }
    
    /**
     * Resets the progress counters and timers.
     * Useful for reusing the same writer for different batches.
     */
    public void reset() {
        itemsProcessed.set(0);
        totalChunks.set(0);
        startTime = LocalDateTime.now();
        lastLogTime = startTime;
    }
    
    /**
     * Gets the total number of items processed so far.
     *
     * @return Count of processed items
     */
    public long getItemsProcessed() {
        return itemsProcessed.get();
    }
    
    /**
     * Gets the total number of chunks processed so far.
     *
     * @return Count of processed chunks
     */
    public long getTotalChunks() {
        return totalChunks.get();
    }
    
    /**
     * Gets the elapsed time since processing started.
     *
     * @return Duration representing elapsed time
     */
    public Duration getElapsedTime() {
        return Duration.between(startTime, LocalDateTime.now());
    }
}