package com.bazarbozorg.typelistservice.batch.exception;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

/**
 * Exception thrown for errors related to the TypeList web service.
 * This is used to wrap both client-side and server-side errors that occur
 * during communication with the TypeList service.
 */
@Setter
@Getter
public class TypeServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * HTTP status code if applicable
     *
     */
    private Integer statusCode;

    /**
     * Constructs a new TypeServiceException with the specified detail message.
     *
     * @param message the detail message
     */
    public TypeServiceException(String message) {
        super(message);
    }

    /**
     * Constructs a new TypeServiceException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public TypeServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new TypeServiceException with the specified detail message and status code.
     *
     * @param message the detail message
     * @param statusCode the HTTP status code
     */
    public TypeServiceException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs a new TypeServiceException with the specified detail message, cause, and status code.
     *
     * @param message the detail message
     * @param cause the cause
     * @param statusCode the HTTP status code
     */
    public TypeServiceException(String message, Throwable cause, Integer statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Indicates whether this exception represents a not found error.
     *
     * @return true if the status code is 404, false otherwise
     */
    public boolean isNotFound() {
        return statusCode != null && statusCode == 404;
    }

    /**
     * Indicates whether this exception represents a client error.
     *
     * @return true if the status code is in the 4xx range, false otherwise
     */
    public boolean isClientError() {
        return statusCode != null && statusCode >= 400 && statusCode < 500;
    }

    /**
     * Indicates whether this exception represents a server error.
     *
     * @return true if the status code is in the 5xx range, false otherwise
     */
    public boolean isServerError() {
        return statusCode != null && statusCode >= 500 && statusCode < 600;
    }
}