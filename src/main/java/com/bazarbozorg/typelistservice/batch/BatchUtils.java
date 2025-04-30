package com.bazarbozorg.typelistservice.batch;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for batch operations such as creating synchronized readers,
 * implementing skip policies for error handling, and file management.
 */
@Getter
@Component
public class BatchUtils {
    private static final Logger log = LoggerFactory.getLogger(BatchUtils.class);
    
    // Counter for skipped items
    private final AtomicInteger skipCounter = new AtomicInteger(0);

    /**
     * -- GETTER --
     *  Gets the input directory path.
     *
     * @return The input directory path
     */
    @Value("${file.input.directory:/data/typelistservice/input/}")
    private String inputDirectory;

    /**
     * -- GETTER --
     *  Gets the archive directory path.
     *
     * @return The archive directory path
     */
    @Getter
    @Value("${file.archive.directory:/data/typelistservice/archive/}")
    private String archiveDirectory;
    
    @Value("${file.error.directory:/data/typelistservice/error/}")
    private String errorDirectory;
    
    /**
     * Creates a synchronized wrapper around an ItemStreamReader for thread safety in multi-threaded steps.
     * 
     * @param reader The original reader to wrap
     * @param <T> The type of items being read
     * @return A synchronized version of the reader
     */
    public <T> SynchronizedItemStreamReader<T> synchronizedItemReader(ItemStreamReader<T> reader) {
        SynchronizedItemStreamReader<T> synchronizedReader = new SynchronizedItemStreamReader<>();
        synchronizedReader.setDelegate(reader);
        return synchronizedReader;
    }
    
    /**
     * Creates a skip policy for file verification that allows skipping items with certain exceptions.
     * This is useful for handling bad records in input files.
     * 
     * @return A SkipPolicy for handling exceptions during file processing
     */
    public SkipPolicy fileVerificationSkipper() {
        return new FileVerificationSkipPolicy();
    }
    
    /**
     * Custom skip policy implementation that determines which exceptions allow skipping an item.
     */
    private class FileVerificationSkipPolicy implements SkipPolicy {
        // Maximum number of items that can be skipped before failing the job
        private static final long MAX_SKIP_COUNT = 1000;
        
        @Override
        public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
            // List of exception types that can be safely skipped
            if (t instanceof FileNotFoundException) {
                return false; // Never skip missing file errors
            }
            
            // Exceptions that can be skipped with caution
            boolean shouldSkip = false;
            
            // Skip parsing and format errors
            if (t instanceof IOException ||
                    t instanceof IllegalArgumentException) {
                shouldSkip = true;
            }
            
            // Check for nested exceptions
            Throwable cause = t.getCause();
            if (cause != null) {
                if (cause instanceof IOException ||
                        cause instanceof IllegalArgumentException) {
                    shouldSkip = true;
                }
            }
            
            // Check if we've exceeded the maximum skip count
            if (shouldSkip) {
                if (skipCount >= MAX_SKIP_COUNT) {
                    log.error("Maximum skip count exceeded ({}). Failing job.", MAX_SKIP_COUNT);
                    assert cause != null;
                    throw new SkipLimitExceededException(MAX_SKIP_COUNT , cause);
                }
                
                // Increment and log skip counter
                int totalSkips = skipCounter.incrementAndGet();
                log.warn("Skipping item due to error: {}, skip count: {}", t.getMessage(), totalSkips);
            }
            
            return shouldSkip;
        }
    }
    
    /**
     * Resets the skip counter, typically called at the start of a new job.
     */
    public void resetSkipCounter() {
        skipCounter.set(0);
    }
    
    /**
     * Gets the current skip count.
     * 
     * @return The number of items skipped
     */
    public int getSkipCount() {
        return skipCounter.get();
    }
    
    /**
     * Retrieves a file from the input directory.
     * 
     * @param fileName The name of the file (without path)
     * @return The File object
     */
    public File getInputFile(String fileName) {
        return new File(inputDirectory + fileName);
    }
    
    /**
     * Retrieves all files from the input directory.
     * 
     * @return Array of Files in the input directory
     */
    public File[] getInputFiles() {
        File dir = new File(inputDirectory);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Input directory does not exist: {}", inputDirectory);
            dir.mkdirs();
            return new File[0];
        }
        return dir.listFiles();
    }
    
    /**
     * Moves a processed file to the archive directory.
     * 
     * @param file The file to archive
     * @throws IOException If there is an error moving the file
     */
    public void archiveFile(File file) throws IOException {
        if (file == null || !file.exists()) {
            log.warn("Cannot archive non-existent file");
            return;
        }
        
        File archiveDir = new File(archiveDirectory);
        if (!archiveDir.exists()) {
            archiveDir.mkdirs();
        }
        
        Path source = file.toPath();
        Path target = Paths.get(archiveDirectory, file.getName());
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("Archived file {} to {}", file.getName(), target);
    }
    
    /**
     * Moves a failed file to the error directory.
     * 
     * @param file The file that failed processing
     * @param exception The exception that occurred during processing
     * @throws IOException If there is an error moving the file
     */
    public void moveToErrorDirectory(File file, Exception exception) throws IOException {
        if (file == null || !file.exists()) {
            log.warn("Cannot move non-existent file to error directory");
            return;
        }
        
        File errorDir = new File(errorDirectory);
        if (!errorDir.exists()) {
            errorDir.mkdirs();
        }
        
        Path source = file.toPath();
        Path target = Paths.get(errorDirectory, file.getName());
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        log.error("Moved file {} to error directory {} due to: {}", 
                file.getName(), target, exception.getMessage());
    }

    /**
     * Ensures all required directories exist, creating them if necessary.
     */
    public void ensureDirectoriesExist() {
        createDirectoryIfNotExists(inputDirectory);
        createDirectoryIfNotExists(archiveDirectory);
        createDirectoryIfNotExists(errorDirectory);
    }
    
    private void createDirectoryIfNotExists(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                log.info("Created directory: {}", directoryPath);
            } else {
                log.warn("Failed to create directory: {}", directoryPath);
            }
        }
    }
}