package com.bazarbozorg.typelistservice.batch.reader;

import com.bazarbozorg.typelistservice.client.model.TypeListDTO;
import org.springframework.batch.item.ItemReader;

import java.io.File;

/**
 * Base interface for reading TypeList data from files.
 * Implementations should handle specific file formats.
 */
public interface TypeListReader {

    /**
     * Creates an ItemReader for TypeList data from the specified file.
     *
     * @param file The input file containing TypeList data
     * @return An ItemReader implementation for TypeList data
     */
    ItemReader<TypeListDTO> createReader(File file);
    
    /**
     * Checks if this reader supports the given file type.
     *
     * @param fileExtension The file extension to check
     * @return true if this reader supports the file type, false otherwise
     */
    boolean supports(String fileExtension);
}