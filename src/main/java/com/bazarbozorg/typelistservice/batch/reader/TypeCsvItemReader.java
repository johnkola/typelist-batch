package com.bazarbozorg.typelistservice.batch.reader;

import com.bazarbozorg.typelistservice.client.model.TypeItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * CSV implementation for reading TypeItem data from files.
 */
@Component
public class TypeCsvItemReader implements TypeItemReader {

    private static final Logger log = LoggerFactory.getLogger(TypeCsvItemReader.class);
    
    @Override
    public ItemReader<TypeItemDTO> createReader(File file) {
        log.info("Creating CSV reader for TypeItem data from: {}", file.getName());
        
        FlatFileItemReader<TypeItemDTO> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(file));
        reader.setEncoding(StandardCharsets.UTF_8.name());
        reader.setLinesToSkip(1); // Skip header row
        reader.setName("typeItemCsvReader");
        
        // Configure line mapper
        LineMapper<TypeItemDTO> lineMapper = createTypeItemLineMapper();
        reader.setLineMapper(lineMapper);
        
        return reader;
    }
    
    @Override
    public boolean supports(String fileExtension) {
        return "csv".equalsIgnoreCase(fileExtension) || "txt".equalsIgnoreCase(fileExtension);
    }
    
    private LineMapper<TypeItemDTO> createTypeItemLineMapper() {
        DefaultLineMapper<TypeItemDTO> lineMapper = new DefaultLineMapper<>();
        
        // Configure tokenizer for CSV structure
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("code", "name", "description", "parentTypeListId", "properties");
        tokenizer.setStrict(false); // Allow lines with missing fields
        
        // Configure mapper for mapping to POJO
        BeanWrapperFieldSetMapper<TypeItemDTO> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(TypeItemDTO.class);
        fieldSetMapper.setCustomEditors(Map.of(UUID.class, new FileReaderFactory.UUIDPropertyEditor()));
        
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        
        return lineMapper;
    }
}