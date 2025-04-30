package com.bazarbozorg.typelistservice.batch.listener;

import com.bazarbozorg.typelistservice.batch.util.MemoryMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Listener for monitoring and logging performance metrics of batch processing.
 * Tracks processing rates, memory usage, and throughput at both step and chunk levels.
 */
@Component  // This annotation is critical for Spring to recognize this as a bean
public class PerformanceMonitoringListener implements StepExecutionListener, ChunkListener {

    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitoringListener.class);
    
    private final ConcurrentHashMap<String, LocalDateTime> stepStartTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> chunkCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastChunkTimes = new ConcurrentHashMap<>();
    private final MemoryMonitor memoryMonitor;
    
    @Value("${batch.performance.log-interval-chunks:10}")
    private int logIntervalChunks;
    
    @Value("${batch.performance.detailed-logging:true}")
    private boolean detailedLogging;

    @Autowired
    public PerformanceMonitoringListener(MemoryMonitor memoryMonitor) {
        this.memoryMonitor = memoryMonitor;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();

        // Initialize counters and timers
        stepStartTimes.put(stepName, LocalDateTime.now());
        chunkCounters.put(stepName, new AtomicLong(0));
        lastChunkTimes.put(stepName, LocalDateTime.now());

        // Log initial memory state
        memoryMonitor.logMemoryUsage("Before step: " + stepName);

        log.info("Starting step: {} with chunk size: {}, throttle-limit: {}",
                stepName,
                stepExecution.getExecutionContext().getInt("batch.chunk-size", -1),
                stepExecution.getExecutionContext().getInt("batch.throttle-limit", -1));
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String stepName = stepExecution.getStepName();
        LocalDateTime startTime = stepStartTimes.get(stepName);
        LocalDateTime endTime = LocalDateTime.now();

        if (startTime != null) {
            // Calculate duration and performance metrics
            Duration duration = Duration.between(startTime, endTime);
            long seconds = duration.getSeconds();

            // Calculate items per second
            long readCount = stepExecution.getReadCount();
            long writeCount = stepExecution.getWriteCount();
            long processSkipCount = stepExecution.getProcessSkipCount();
            long readSkipCount = stepExecution.getReadSkipCount();
            long writeSkipCount = stepExecution.getWriteSkipCount();
            long totalSkipCount = processSkipCount + readSkipCount + writeSkipCount;

            // Avoid division by zero
            double readPerSecond = seconds > 0 ? (double) readCount / seconds : 0;
            double writePerSecond = seconds > 0 ? (double) writeCount / seconds : 0;

            // Log performance summary
            log.info("Performance summary for step {}", stepName);
            log.info("Duration: {} seconds", seconds);
            log.info("Read count: {}, rate: {:.2f} items/sec", readCount, readPerSecond);
            log.info("Write count: {}, rate: {:.2f} items/sec", writeCount, writePerSecond);
            log.info("Skip count: {} (read: {}, process: {}, write: {})",
                    totalSkipCount, readSkipCount, processSkipCount, writeSkipCount);

            // Log final memory state
            memoryMonitor.logMemoryUsage("After step: " + stepName);

            // Log any additional step metrics
            if (detailedLogging) {
                logAdditionalStepMetrics(stepExecution);
            }

            // Cleanup
            stepStartTimes.remove(stepName);
            chunkCounters.remove(stepName);
            lastChunkTimes.remove(stepName);
        }

        return null; // Return null to indicate no change to the exit status
    }

    @Override
    public void beforeChunk(ChunkContext context) {
        // Nothing to do before chunk processing
    }

    @Override
    public void afterChunk(ChunkContext context) {
        StepExecution stepExecution = context.getStepContext().getStepExecution();
        String stepName = stepExecution.getStepName();

        // Increment chunk counter
        AtomicLong counter = chunkCounters.computeIfAbsent(stepName, k -> new AtomicLong(0));
        long currentChunk = counter.incrementAndGet();

        // Only log at specified intervals to avoid excessive logging
        if (currentChunk % logIntervalChunks == 0) {
            // Calculate chunk metrics
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastTime = lastChunkTimes.get(stepName);

            if (lastTime != null) {
                Duration chunkDuration = Duration.between(lastTime, now);
                long chunkMillis = chunkDuration.toMillis();

                // Get chunk size from the step execution context or use default
                int chunkSize = context.getStepContext().getStepExecution().getExecutionContext()
                        .getInt("batch.chunk-size", 0);

                // Log if chunkSize is known
                if (chunkSize > 0 && chunkMillis > 0) {
                    double itemsPerSecond = (chunkSize * 1000.0) / chunkMillis;
                    log.info("Chunk {} completed for step {}: {} items in {} ms ({} items/sec)",
                            currentChunk, stepName, chunkSize, chunkMillis, 
                            String.format("%.2f", itemsPerSecond));
                } else {
                    log.info("Chunk {} completed for step {}", currentChunk, stepName);
                }

                // Periodically log memory usage
                if (currentChunk % (logIntervalChunks * 5L) == 0) {
                    memoryMonitor.logMemoryUsage("During step: " + stepName + ", chunk: " + currentChunk);
                }
            }

            // Update last chunk time
            lastChunkTimes.put(stepName, now);
        }
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        StepExecution stepExecution = context.getStepContext().getStepExecution();
        String stepName = stepExecution.getStepName();

        // Log error details
        log.error("Error in chunk processing for step: {}", stepName);

        // Log memory state at error
        memoryMonitor.logMemoryUsage("Chunk error in step: " + stepName);
    }

    /**
     * Logs additional detailed metrics about the step execution
     */
    private void logAdditionalStepMetrics(StepExecution stepExecution) {
        // Log CPU and system metrics if available
        try {
            Runtime runtime = Runtime.getRuntime();
            int availableProcessors = runtime.availableProcessors();

            log.info("System information - Available processors: {}", availableProcessors);

            // Log thread information if available
            ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
            ThreadGroup parentGroup;
            while ((parentGroup = rootGroup.getParent()) != null) {
                rootGroup = parentGroup;
            }
            int activeThreads = rootGroup.activeCount();
            log.info("Active threads: {}", activeThreads);

            // Log filter/commit counts
            log.info("Filter count: {}", stepExecution.getFilterCount());
            log.info("Commit count: {}", stepExecution.getCommitCount());
            log.info("Rollback count: {}", stepExecution.getRollbackCount());
        } catch (Exception e) {
            log.warn("Error collecting additional metrics", e);
        }
    }
}