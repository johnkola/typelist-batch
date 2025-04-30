package com.bazarbozorg.typelistservice.batch.config;

import com.bazarbozorg.typelistservice.batch.BatchUtils;
import com.bazarbozorg.typelistservice.batch.reader.FileReaderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * Scheduler for automatic batch job execution based on configured cron expression.
 */
@Slf4j
@Configuration
@EnableScheduling
public class BatchJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job typeListBatchJob;
    private final FileReaderFactory fileReaderFactory;
    private final BatchUtils batchUtils;
    @Value("${batch.schedule.enabled:false}")
    private boolean schedulingEnabled;
    @Value("${batch.process-on-startup:false}")
    private boolean processOnStartup;

    /**
     * Creates a new BatchJobScheduler.
     *
     * @param jobLauncher       The job launcher
     * @param typeListBatchJob  The batch job to run
     * @param fileReaderFactory Factory for finding files to process
     * @param batchUtils        Utilities for batch operations
     */
    @Autowired
    public BatchJobScheduler(
            JobLauncher jobLauncher,
            @Qualifier("importDataJob") Job typeListBatchJob,
            FileReaderFactory fileReaderFactory,
            BatchUtils batchUtils) {
        this.jobLauncher = jobLauncher;
        this.typeListBatchJob = typeListBatchJob;
        this.fileReaderFactory = fileReaderFactory;
        this.batchUtils = batchUtils;

        // Process on startup if configured
        if (processOnStartup) {
            log.info("Process on startup enabled - scheduling immediate job execution");
            new Thread(this::processAllPendingFiles).start();
        }
    }

    /**
     * Scheduled method to check and process files based on the configured cron expression.
     * The cron expression is defined in application.properties as batch.schedule.cron.
     */
    @Scheduled(cron = "${batch.schedule.cron:0 0 * * * *}")
    public void scheduledFileProcessing() {
        if (!schedulingEnabled) {
            log.debug("Scheduled processing disabled, skipping execution");
            return;
        }

        log.info("Running scheduled file processing job");
        processAllPendingFiles();
    }

    /**
     * Processes all pending files in the input directory.
     */
    public void processAllPendingFiles() {
        try {
            // Create directories if they don't exist
            batchUtils.ensureDirectoriesExist();

            // Process TypeList files
            List<File> typeListFiles = fileReaderFactory.findTypeListFiles();
            for (File file : typeListFiles) {
                launchJob(file, "TYPELIST");
            }

            // Process TypeItem files
            List<File> typeItemFiles = fileReaderFactory.findTypeItemFiles();
            for (File file : typeItemFiles) {
                launchJob(file, "TYPEITEM");
            }

            // Process TypeSet files
            List<File> typeSetFiles = fileReaderFactory.findTypeSetFiles();
            for (File file : typeSetFiles) {
                launchJob(file, "TYPESET");
            }

            if (typeListFiles.isEmpty() && typeItemFiles.isEmpty() && typeSetFiles.isEmpty()) {
                log.info("No files found for processing");
            }
        } catch (Exception e) {
            log.error("Error during scheduled file processing", e);
        }
    }

    /**
     * Launches a job for a specific file.
     *
     * @param file     The file to process
     * @param fileType The type of the file
     */
    private void launchJob(File file, String fileType) {
        try {
            log.info("Launching job for file: {} (type: {})", file.getName(), fileType);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", file.getAbsolutePath())
                    .addString("fileType", fileType)
                    .addDate("runTime", new Date())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(typeListBatchJob, jobParameters);
            log.info("Job execution status: {}", execution.getStatus());
        } catch (Exception e) {
            log.error("Error launching job for file: {}", file.getName(), e);
            try {
                batchUtils.moveToErrorDirectory(file, e);
            } catch (Exception ex) {
                log.error("Could not move file to error directory: {}", file.getName(), ex);
            }
        }
    }

    /**
     * Process a specific file.
     *
     * @param file     The file to process
     * @param fileType The type of file (TYPELIST, TYPEITEM, TYPESET)
     */
    public void processFile(File file, String fileType) {
        launchJob(file, fileType);
    }
}