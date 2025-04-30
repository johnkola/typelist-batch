package com.bazarbozorg.typelistservice.batch.controller;

import com.bazarbozorg.typelistservice.batch.BatchUtils;
import com.bazarbozorg.typelistservice.batch.config.BatchJobScheduler;
import com.bazarbozorg.typelistservice.batch.reader.FileReaderFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

/**
 * REST controller for manually triggering batch jobs.
 */
@RestController
@RequestMapping("/api/batch")
@Tag(name = "Batch Operations", description = "API endpoints for managing and triggering batch processes")
public class BatchJobController {
    private static final Logger log = LoggerFactory.getLogger(BatchJobController.class);

    private final BatchJobScheduler batchJobScheduler;
    private final FileReaderFactory fileReaderFactory;
    private final BatchUtils batchUtils;
    private final Job parallelTypeListJob;

    /**
     * Creates a new BatchJobController.
     *
     * @param batchJobScheduler The batch job scheduler
     * @param fileReaderFactory Factory for finding files
     * @param batchUtils        Utilities for batch operations
     */
    @Autowired
    public BatchJobController(
            BatchJobScheduler batchJobScheduler,
            FileReaderFactory fileReaderFactory,
            BatchUtils batchUtils, Job parallelTypeListJob) {
        this.batchJobScheduler = batchJobScheduler;
        this.fileReaderFactory = fileReaderFactory;
        this.batchUtils = batchUtils;
        this.parallelTypeListJob = parallelTypeListJob;
    }

    /**
     * Process all pending files.
     *
     * @return Response entity with status message
     */
    @Operation(
            summary = "Process all pending files",
            description = "Triggers batch processing for all TypeList, TypeItem, and TypeSet files found in the input directory"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Processing has been initiated",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            )
    })
    @PostMapping("/process/all")
    public ResponseEntity<String> processAllFiles() {
        log.info("Manual trigger: process all files");
        batchJobScheduler.processAllPendingFiles();
        return ResponseEntity.ok("Processing all files initiated");
    }

    /**
     * Process only TypeList files.
     *
     * @return Response entity with status message
     */
    @Operation(
            summary = "Process TypeList files only",
            description = "Triggers batch processing for only TypeList files found in the input directory"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Processing has been initiated",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            )
    })
    @PostMapping("/process/typelist")
    public ResponseEntity<String> processTypeListFiles() {
        log.info("Manual trigger: process TypeList files");

        fileReaderFactory.findTypeListFiles().forEach(file -> {
            // Delegate to the scheduler to launch the job
            batchJobScheduler.processFile(file, "TYPELIST");
        });

        return ResponseEntity.ok("Processing TypeList files initiated");
    }

    /**
     * Process only TypeItem files.
     *
     * @return Response entity with status message
     */
    @Operation(
            summary = "Process TypeItem files only",
            description = "Triggers batch processing for only TypeItem files found in the input directory"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Processing has been initiated",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            )
    })
    @PostMapping("/process/typeitem")
    public ResponseEntity<String> processTypeItemFiles() {
        log.info("Manual trigger: process TypeItem files");

        fileReaderFactory.findTypeItemFiles().forEach(file -> {
            batchJobScheduler.processFile(file, "TYPEITEM");
        });

        return ResponseEntity.ok("Processing TypeItem files initiated");
    }

    /**
     * Process only TypeSet files.
     *
     * @return Response entity with status message
     */
    @Operation(
            summary = "Process TypeSet files only",
            description = "Triggers batch processing for only TypeSet files found in the input directory"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Processing has been initiated",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            )
    })
    @PostMapping("/process/typeset")
    public ResponseEntity<String> processTypeSetFiles() {
        log.info("Manual trigger: process TypeSet files");

        fileReaderFactory.findTypeSetFiles().forEach(file -> {
            batchJobScheduler.processFile(file, "TYPESET");
        });

        return ResponseEntity.ok("Processing TypeSet files initiated");
    }

    /**
     * Process a specific file.
     *
     * @param path The path to the file
     * @return Response entity with status message
     */
    @Operation(
            summary = "Process a specific file",
            description = "Triggers batch processing for a specific file identified by its path. The file name must start with 'typelist_', 'typeitem_', or 'typeset_'."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Processing has been initiated",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request - file not found or unrecognized file type",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            )
    })
    @PostMapping("/process/file")
    public ResponseEntity<String> processFile(
            @Parameter(description = "Full path to the file to process", required = true)
            @RequestParam String path) {
        log.info("Manual trigger: process specific file: {}", path);

        File file = new File(path);
        if (!file.exists()) {
            return ResponseEntity.badRequest().body("File not found: " + path);
        }

        String fileName = file.getName().toLowerCase();
        String fileType;

        if (fileName.startsWith("typelist_")) {
            fileType = "TYPELIST";
        } else if (fileName.startsWith("typeitem_")) {
            fileType = "TYPEITEM";
        } else if (fileName.startsWith("typeset_")) {
            fileType = "TYPESET";
        } else {
            return ResponseEntity.badRequest().body("Unrecognized file type: " + fileName);
        }

        batchJobScheduler.processFile(file, fileType);
        return ResponseEntity.ok("Processing file initiated: " + path);
    }

    /**
     * Get the current status of batch processing.
     *
     * @return Response entity with status information
     */
    @Operation(
            summary = "Get batch processing status",
            description = "Retrieves the current status of all batch processing jobs"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Status information retrieved successfully",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "text/plain", schema = @Schema(type = "string"))
            )
    })
    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        // Implement status reporting based on your batch job execution repository
        return ResponseEntity.ok("Status information would be provided here");
    }

}