package com.bazarbozorg.typelistservice.batch.reader;

import com.bazarbozorg.typelistservice.client.model.TypeItemDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.batch.item.json.JsonItemReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * JSON implementation for reading TypeItem data from files.
 */
@Component
public class TypeJsonItemReader implements TypeItemReader {

    private static final Logger log = LoggerFactory.getLogger(TypeJsonItemReader.class);
    private final ObjectMapper objectMapper;
    
    public TypeJsonItemReader() {
        this.objectMapper = new ObjectMapper();
        // Configure object mapper
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Override
    public ItemReader<TypeItemDTO> createReader(File file) {
        log.info("Creating JSON reader for TypeItem data from: {}", file.getName());
        
        JacksonJsonObjectReader<TypeItemDTO> jsonObjectReader = new JacksonJsonObjectReader<>(TypeItemDTO.class);
        jsonObjectReader.setMapper(objectMapper);
        
        JsonItemReader<TypeItemDTO> reader = new JsonItemReader<>();
        reader.setResource(new FileSystemResource(file));
        reader.setJsonObjectReader(jsonObjectReader);
        reader.setName("typeItemJsonReader");
        
        return reader;
    }
    
    @Override
    public boolean supports(String fileExtension) {
        return "json".equalsIgnoreCase(fileExtension);
    }
}