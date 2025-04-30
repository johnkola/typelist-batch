package com.bazarbozorg.typelistservice.batch.config;

import com.bazarbozorg.typelistservice.batch.reader.FileReaderFactory;
import com.bazarbozorg.typelistservice.client.model.TypeItemDTO;
import com.bazarbozorg.typelistservice.client.model.TypeListDTO;
import com.bazarbozorg.typelistservice.client.model.TypeSetDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

/**
 * Configuration for file readers used by both sequential and parallel batch configurations
 */
@Configuration
public class ReaderConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ReaderConfiguration.class);

    @Autowired
    private FileReaderFactory fileReaderFactory;

    /**
     * Reader for TypeList files
     */
    @Bean
    @StepScope
    @Qualifier("typeListReader")
    public ItemReader<TypeListDTO> typeListReader(
            @Value("#{jobParameters['typeListFileName']}") String paramFileName) {
        try {
            // Case 1: Use file specified in job parameters if available
            if (paramFileName != null && !paramFileName.isEmpty()) {
                File file = Paths.get(fileReaderFactory.getBatchUtils().getInputDirectory(), paramFileName).toFile();
                log.info("Using specified TypeList file from job parameters: {}", file.getAbsolutePath());
                return fileReaderFactory.createTypeListReader(file);
            }

            // Case 2: Look for matching files in input directory
            List<File> typeListFiles = fileReaderFactory.findTypeListFiles();
            if (!typeListFiles.isEmpty()) {
                log.info("Found TypeList file: {}", typeListFiles.get(0).getName());
                return fileReaderFactory.createTypeListReader(typeListFiles.get(0));
            }

            // Case 3: Fall back to system property
            String fileName = System.getProperty("typelist.file.name");
            if (fileName != null && !fileName.isEmpty()) {
                File file = Paths.get(fileReaderFactory.getBatchUtils().getInputDirectory(), fileName).toFile();
                log.info("Using TypeList file from system property: {}", file.getAbsolutePath());
                return fileReaderFactory.createTypeListReader(file);
            }

            // Case 4: Use default configured in application properties
            String defaultFile = "typelist_" + System.currentTimeMillis() + ".json";
            File file = Paths.get(fileReaderFactory.getBatchUtils().getInputDirectory(), defaultFile).toFile();
            log.info("No TypeList files found, defaulting to: {}", file.getAbsolutePath());
            return fileReaderFactory.createTypeListReader(file);
        } catch (Exception e) {
            log.error("Error creating TypeList reader", e);
            return null;
        }
    }

    /**
     * Reader for TypeItem files
     */
    @Bean
    @StepScope
    @Qualifier("typeItemReader")
    public ItemReader<TypeItemDTO> typeItemReader(
            @Value("#{jobParameters['typeItemFileName']}") String paramFileName) {
        try {
            // Case 1: Use file specified in job parameters if available
            if (paramFileName != null && !paramFileName.isEmpty()) {
                File file = Paths.get(fileReaderFactory.getBatchUtils().getInputDirectory(), paramFileName).toFile();
                log.info("Using specified TypeItem file from job parameters: {}", file.getAbsolutePath());
                return fileReaderFactory.createTypeItemReader(file);
            }

            // Case 2: Look for matching files in input directory
            List<File> typeItemFiles = fileReaderFactory.findTypeItemFiles();
            if (!typeItemFiles.isEmpty()) {
                log.info("Found TypeItem file: {}", typeItemFiles.get(0).getName());
                return fileReaderFactory.createTypeItemReader(typeItemFiles.get(0));
            }

            // Case 3: Fall back to system property
            String fileName = System.getProperty("typeitem.file.name");
            if (fileName != null && !fileName.isEmpty()) {
                File file = Paths.get(fileReaderFactory.getBatchUtils().getInputDirectory(), fileName).toFile();
                log.info("Using TypeItem file from system property: {}", file.getAbsolutePath());
                return fileReaderFactory.createTypeItemReader(file);
            }

            // Case 4: Use default configured in application properties
            String defaultFile = "typeitem_" + System.currentTimeMillis() + ".json";
            File file = Paths.get(fileReaderFactory.getBatchUtils().getInputDirectory(), defaultFile).toFile();
            log.info("No TypeItem files found, defaulting to: {}", file.getAbsolutePath());
            return fileReaderFactory.createTypeItemReader(file);
        } catch (Exception e) {
            log.error("Error creating TypeItem reader", e);
            return null;
        }
    }

    /**
     * Reader for TypeSet files
     */
    @Bean
    @StepScope
    @Qualifier("typeSetReader")
    public ItemReader<TypeSetDTO> typeSetReader(
            @Value("#{jobParameters['typeSetFileName']}") String paramFileName) {
        try {
            // Case 1: Use file specified in job parameters if available
            if (paramFileName != null && !paramFileName.isEmpty()) {
                File file = Paths.get(fileReaderFactory.getBatchUtils().getInputDirectory(), paramFileName).toFile();
                log.info("Using specified TypeSet file from job parameters: {}", file.getAbsolutePath());
                return fileReaderFactory.createTypeSetReader(file);
            }

            // Case 2: Look for matching files in input directory
            List<File> typeSetFiles = fileReaderFactory.findTypeSetFiles();
            if (!typeSetFiles.isEmpty()) {
                log.info("Found TypeSet file: {}", typeSetFiles.get(0).getName());
                return fileReaderFactory.createTypeSetReader(typeSetFiles.get(0));
            }

            // Case 3: Fall back to system property
            String fileName = System.getProperty("typeset.file.name");
            if (fileName != null && !fileName.isEmpty()) {
                File file = Paths.get(fileReaderFactory.getBatchUtils().getInputDirectory(), fileName).toFile();
                log.info("Using TypeSet file from system property: {}", file.getAbsolutePath());
                return fileReaderFactory.createTypeSetReader(file);
            }

            // Case 4: Use default configured in application properties
            String defaultFile = "typeset_" + System.currentTimeMillis() + ".json";
            File file = Paths.get(fileReaderFactory.getBatchUtils().getInputDirectory(), defaultFile).toFile();
            log.info("No TypeSet files found, defaulting to: {}", file.getAbsolutePath());
            return fileReaderFactory.createTypeSetReader(file);
        } catch (Exception e) {
            log.error("Error creating TypeSet reader", e);
            return null;
        }
    }
}