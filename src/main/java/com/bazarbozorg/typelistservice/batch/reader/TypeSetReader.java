package com.bazarbozorg.typelistservice.batch.reader;

import com.bazarbozorg.typelistservice.client.model.TypeSetDTO;
import org.springframework.batch.item.ItemReader;

import java.io.File;

/**
 * Base interface for reading TypeSet data from files.
 * Implementations should handle specific file formats.
 */
public interface TypeSetReader {

    /**
     * Creates an ItemReader for TypeSet data from the specified file.
     *
     * @param file The input file containing TypeSet data
     * @return An ItemReader implementation for TypeSet data
     */
    ItemReader<TypeSetDTO> createReader(File file);
    
    /**
     * Checks if this reader supports the given file type.
     *
     * @param fileExtension The file extension to check
     * @return true if this reader supports the file type, false otherwise
     */
    boolean supports(String fileExtension);
}