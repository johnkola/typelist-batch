package com.bazarbozorg.typelistservice.batch.reader;

import com.bazarbozorg.typelistservice.batch.BatchUtils;
import com.bazarbozorg.typelistservice.batch.exception.FileProcessingException;
import com.bazarbozorg.typelistservice.client.model.TypeItemDTO;
import com.bazarbozorg.typelistservice.client.model.TypeListDTO;
import com.bazarbozorg.typelistservice.client.model.TypeSetDTO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Factory for creating appropriate ItemReader implementations based on file type.
 * Supports CSV, JSON, and Excel files for TypeList, TypeItem, and TypeSet entities.
 */
@Slf4j
@Component
public class FileReaderFactory {


    private final List<TypeListReader> typeListReaders;
    private final List<TypeItemReader> typeItemReaders;
    private final List<TypeSetReader> typeSetReaders;
    /**
     * -- GETTER --
     * Gets the batch utilities
     *
     * @return The BatchUtils instance
     */
    @Getter
    private final BatchUtils batchUtils;

    /**
     * Constructs a new FileReaderFactory with the specified readers and utility classes.
     *
     * @param typeListReaders List of TypeList readers
     * @param typeItemReaders List of TypeItem readers
     * @param typeSetReaders  List of TypeSet readers
     * @param batchUtils      Batch utilities for file handling
     */
    @Autowired
    public FileReaderFactory(
            List<TypeListReader> typeListReaders,
            List<TypeItemReader> typeItemReaders,
            List<TypeSetReader> typeSetReaders,
            BatchUtils batchUtils) {
        this.typeListReaders = typeListReaders;
        this.typeItemReaders = typeItemReaders;
        this.typeSetReaders = typeSetReaders;
        this.batchUtils = batchUtils;

        // Ensure required directories exist
        this.batchUtils.ensureDirectoriesExist();
    }

    /**
     * Creates an appropriate ItemReader for TypeList data based on file type
     *
     * @param file The input file containing TypeList data
     * @return An ItemReader implementation for TypeList data
     * @throws FileProcessingException if no suitable reader is found for the file type
     */
    public ItemReader<TypeListDTO> createTypeListReader(File file) {
        String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
        log.info("Creating TypeList reader for file: {} (type: {})", file.getName(), extension);

        for (TypeListReader reader : typeListReaders) {
            if (reader.supports(extension)) {
                return reader.createReader(file);
            }
        }

        throw new FileProcessingException(
                "Unsupported file type for TypeList",
                file.getName(),
                extension
        );
    }

    /**
     * Creates an appropriate ItemReader for TypeItem data based on file type
     *
     * @param file The input file containing TypeItem data
     * @return An ItemReader implementation for TypeItem data
     * @throws FileProcessingException if no suitable reader is found for the file type
     */
    public ItemReader<TypeItemDTO> createTypeItemReader(File file) {
        String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
        log.info("Creating TypeItem reader for file: {} (type: {})", file.getName(), extension);

        for (TypeItemReader reader : typeItemReaders) {
            if (reader.supports(extension)) {
                return reader.createReader(file);
            }
        }

        throw new FileProcessingException(
                "Unsupported file type for TypeItem",
                file.getName(),
                extension
        );
    }

    /**
     * Creates an appropriate ItemReader for TypeSet data based on file type
     *
     * @param file The input file containing TypeSet data
     * @return An ItemReader implementation for TypeSet data
     * @throws FileProcessingException if no suitable reader is found for the file type
     */
    public ItemReader<TypeSetDTO> createTypeSetReader(File file) {
        String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
        log.info("Creating TypeSet reader for file: {} (type: {})", file.getName(), extension);

        for (TypeSetReader reader : typeSetReaders) {
            if (reader.supports(extension)) {
                return reader.createReader(file);
            }
        }

        throw new FileProcessingException(
                "Unsupported file type for TypeSet",
                file.getName(),
                extension
        );
    }

    /**
     * Finds TypeList files in the input directory that match the naming pattern
     *
     * @return List of TypeList files found
     */
    public List<File> findTypeListFiles() {
        return findFilesWithPrefix("typelist_");
    }

    /**
     * Finds TypeItem files in the input directory that match the naming pattern
     *
     * @return List of TypeItem files found
     */
    public List<File> findTypeItemFiles() {
        return findFilesWithPrefix("typeitem_");
    }

    /**
     * Finds TypeSet files in the input directory that match the naming pattern
     *
     * @return List of TypeSet files found
     */
    public List<File> findTypeSetFiles() {
        return findFilesWithPrefix("typeset_");
    }

    /**
     * Finds files in the input directory that match a specific prefix
     *
     * @param prefix The filename prefix to match
     * @return List of matching files
     */
    private List<File> findFilesWithPrefix(String prefix) {
        File[] allFiles = batchUtils.getInputFiles();
        if (allFiles == null || allFiles.length == 0) {
            log.info("No files found in input directory: {}{}*.*", batchUtils.getInputDirectory(), prefix);
            return List.of();
        }

        return Stream.of(allFiles)
                .filter(file -> file.isFile() && file.getName().toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Custom property editor for converting UUID strings to UUID objects
     */
    public static class UUIDPropertyEditor extends java.beans.PropertyEditorSupport {
        @Override
        public void setAsText(String text) {
            if (text == null || text.trim().isEmpty()) {
                setValue(null);
            } else {
                setValue(java.util.UUID.fromString(text.trim()));
            }
        }
    }
}