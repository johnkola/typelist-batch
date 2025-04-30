package com.bazarbozorg.typelistservice.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;


/**
 * Configuration for job launching and a simple REST endpoint to trigger jobs.
 */
@Slf4j
@Configuration
@RestController
public class JobLauncherConfig {

    private final JobRepository jobRepository;
    private final Job importDataJob;
    private final Job parallelTypeListJob;
    private final Job partitionedTypeItemJob;

    @Autowired
    public JobLauncherConfig(
            JobRepository jobRepository,
            @Autowired(required = false) Job importDataJob,
            @Autowired(required = false) @Qualifier("parallelTypeListJob") Job parallelTypeListJob,
            @Autowired(required = false) @Qualifier("partitionedTypeItemJob") Job partitionedTypeItemJob) {
        this.jobRepository = jobRepository;
        this.importDataJob = importDataJob;
        this.parallelTypeListJob = parallelTypeListJob;
        this.partitionedTypeItemJob = partitionedTypeItemJob;
    }

    /**
     * Creates an asynchronous job launcher to run batch jobs in the background
     */
    @Bean
    public JobLauncher asyncJobLauncher() throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor());
        launcher.afterPropertiesSet();
        return launcher;
    }

    /**
     * REST endpoint to trigger the sequential import job
     */
    @GetMapping("/run-import-job")
    public String runImportJob() {
        if (importDataJob == null) {
            return "Import job is not available in the current configuration";
        }
        return launchJob(importDataJob, "importDataJob");
    }

    /**
     * REST endpoint to trigger the parallel processing job
     */
    @GetMapping("/run-parallel-job")
    public String runParallelJob() {
        if (parallelTypeListJob == null) {
            return "Parallel job is not available in the current configuration";
        }
        return launchJob(parallelTypeListJob, "parallelTypeListJob");
    }

    /**
     * REST endpoint to trigger the partitioned job
     */
    @GetMapping("/run-partitioned-job")
    public String runPartitionedJob() {
        if (partitionedTypeItemJob == null) {
            return "Partitioned job is not available in the current configuration";
        }
        return launchJob(partitionedTypeItemJob, "partitionedTypeItemJob");
    }

    /**
     * Helper method to launch a job with common parameter building
     */
    private String launchJob(Job job, String jobName) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addDate("runDate", new Date())
                    .addString("jobName", jobName)
                    .toJobParameters();

            asyncJobLauncher().run(job, params);

            return "Job " + jobName + " launched successfully. Check logs for details.";
        } catch (Exception e) {
            log.error("Error launching job {}: {}", jobName, e.getMessage(), e);
            return "Error launching job: " + e.getMessage();
        }
    }
}