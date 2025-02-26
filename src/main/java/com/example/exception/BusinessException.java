package com.example.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom exception for business logic errors
 * Includes HTTP status code to be returned to the client
 */
@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public BusinessException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }
    
    public BusinessException(String message, Throwable cause) {
        this(message, HttpStatus.BAD_REQUEST, cause);
    }
} 