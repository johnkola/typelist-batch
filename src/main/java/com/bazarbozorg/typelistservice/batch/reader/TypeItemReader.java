package com.bazarbozorg.typelistservice.batch.reader;

import com.bazarbozorg.typelistservice.client.model.TypeItemDTO;
import org.springframework.batch.item.ItemReader;

import java.io.File;

/**
 * Base interface for reading TypeItem data from files.
 * Implementations should handle specific file formats.
 */
public interface TypeItemReader {

    /**
     * Creates an ItemReader for TypeItem data from the specified file.
     *
     * @param file The input file containing TypeItem data
     * @return An ItemReader implementation for TypeItem data
     */
    ItemReader<TypeItemDTO> createReader(File file);
    
    /**
     * Checks if this reader supports the given file type.
     *
     * @param fileExtension The file extension to check
     * @return true if this reader supports the file type, false otherwise
     */
    boolean supports(String fileExtension);
}