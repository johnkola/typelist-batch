package com.bazarbozorg.typelistservice.batch.exception;

import java.io.Serial;

/**
 * Exception for file processing errors.
 * Handles issues with file reading or parsing.
 */
public class FileProcessingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;
    
    private final String fileName;
    private final String fileType;
    
    /**
     * Constructs a new FileProcessingException with the specified detail message.
     *
     * @param message the detail message
     */
    public FileProcessingException(String message) {
        super(message);
        this.fileName = null;
        this.fileType = null;
    }
    
    /**
     * Constructs a new FileProcessingException with the specified detail message and file information.
     *
     * @param message the detail message
     * @param fileName the name of the file being processed
     * @param fileType the type of the file being processed
     */
    public FileProcessingException(String message, String fileName, String fileType) {
        super(message);
        this.fileName = fileName;
        this.fileType = fileType;
    }
    
    /**
     * Constructs a new FileProcessingException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.fileName = null;
        this.fileType = null;
    }
    
    /**
     * Constructs a new FileProcessingException with the specified detail message, cause, and file information.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     * @param fileName the name of the file being processed
     * @param fileType the type of the file being processed
     */
    public FileProcessingException(String message, Throwable cause, String fileName, String fileType) {
        super(message, cause);
        this.fileName = fileName;
        this.fileType = fileType;
    }
    
    /**
     * Gets the name of the file that caused the exception.
     *
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Gets the type of the file that caused the exception.
     *
     * @return the file type
     */
    public String getFileType() {
        return fileType;
    }
    
    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder(super.getMessage());
        
        if (fileName != null) {
            message.append(" [File: ").append(fileName);
            
            if (fileType != null) {
                message.append(", Type: ").append(fileType);
            }
            
            message.append("]");
        }
        
        return message.toString();
    }
}