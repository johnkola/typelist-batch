package com.bazarbozorg.typelistservice.batch.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class FilePathConfiguration {

    // Getters
    @Value("${file.input.directory:/data/typelistservice/input/}")
    private String inputDirectory;
    
    @Value("${file.archive.directory:/data/typelistservice/archive/}")
    private String archiveDirectory;
    
    @Value("${file.error.directory:/data/typelistservice/error/}")
    private String errorDirectory;

}