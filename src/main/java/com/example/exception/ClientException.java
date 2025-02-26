package com.example.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom exception for client-related errors
 * Used when communicating with external services
 */
@Getter
public class ClientException extends BusinessException {
    private final String clientName;
    private final String endpoint;

    public ClientException(String message, HttpStatus status, String clientName, String endpoint) {
        super(message, status);
        this.clientName = clientName;
        this.endpoint = endpoint;
    }
    
    public ClientException(String message, HttpStatus status, String clientName, String endpoint, Throwable cause) {
        super(message, status, cause);
        this.clientName = clientName;
        this.endpoint = endpoint;
    }

    public ClientException(String message, String clientName, String endpoint) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR, clientName, endpoint);
    }
    
    public ClientException(String message, String clientName, String endpoint, Throwable cause) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR, clientName, endpoint, cause);
    }
} 