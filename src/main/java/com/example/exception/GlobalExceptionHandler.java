package com.example.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import javax.persistence.EntityNotFoundException;
import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API
 * Handles all exceptions and returns a standardized response
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getDescription(false).substring(4))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getDescription(false).substring(4))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .validationErrors(errors)
                .path(request.getDescription(false).substring(4))
                .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        Map<String, String> details = new HashMap<>();
        
        // Add cause information if available
        if (ex.getCause() != null) {
            details.put("cause", ex.getCause().getMessage());
            details.put("causeType", ex.getCause().getClass().getName());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getDescription(false).substring(4))
                .validationErrors(details.isEmpty() ? null : details)
                .build();
        
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }
    
    /**
     * Handle client-specific exceptions
     */
    @ExceptionHandler(ClientException.class)
    public ResponseEntity<ErrorResponse> handleClientException(ClientException ex, WebRequest request) {
        Map<String, String> details = new HashMap<>();
        details.put("clientName", ex.getClientName());
        details.put("endpoint", ex.getEndpoint());
        
        // Add cause information if available
        if (ex.getCause() != null) {
            details.put("cause", ex.getCause().getMessage());
            details.put("causeType", ex.getCause().getClass().getName());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message("Client error: " + ex.getMessage())
                .path(request.getDescription(false).substring(4))
                .validationErrors(details)
                .build();
        
        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }
    
    /**
     * Handle HTTP client error exceptions (4xx)
     */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpClientErrorException(HttpClientErrorException ex, WebRequest request) {
        Map<String, String> details = new HashMap<>();
        details.put("responseBody", ex.getResponseBodyAsString());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatusCode().value())
                .error(ex.getStatusCode().getReasonPhrase())
                .message("Client error: " + ex.getMessage())
                .path(request.getDescription(false).substring(4))
                .validationErrors(details)
                .build();
        
        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }
    
    /**
     * Handle HTTP server error exceptions (5xx)
     */
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<ErrorResponse> handleHttpServerErrorException(HttpServerErrorException ex, WebRequest request) {
        Map<String, String> details = new HashMap<>();
        details.put("responseBody", ex.getResponseBodyAsString());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatusCode().value())
                .error(ex.getStatusCode().getReasonPhrase())
                .message("Server error: " + ex.getMessage())
                .path(request.getDescription(false).substring(4))
                .validationErrors(details)
                .build();
        
        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }
    
    /**
     * Handle resource access exceptions (connection issues)
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(ResourceAccessException ex, WebRequest request) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        
        Map<String, String> details = new HashMap<>();
        if (ex.getCause() != null) {
            details.put("cause", ex.getCause().getMessage());
            details.put("causeType", ex.getCause().getClass().getName());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Service unavailable: " + ex.getMessage())
                .path(request.getDescription(false).substring(4))
                .validationErrors(details.isEmpty() ? null : details)
                .build();
        
        return new ResponseEntity<>(errorResponse, status);
    }
    
    /**
     * Handle connection exceptions
     */
    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ErrorResponse> handleConnectException(ConnectException ex, WebRequest request) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Connection failed: " + ex.getMessage())
                .path(request.getDescription(false).substring(4))
                .build();
        
        return new ResponseEntity<>(errorResponse, status);
    }
    
    /**
     * Handle general REST client exceptions
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleRestClientException(RestClientException ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        
        Map<String, String> details = new HashMap<>();
        if (ex.getCause() != null) {
            details.put("cause", ex.getCause().getMessage());
            details.put("causeType", ex.getCause().getClass().getName());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("REST client error: " + ex.getMessage())
                .path(request.getDescription(false).substring(4))
                .validationErrors(details.isEmpty() ? null : details)
                .build();
        
        return new ResponseEntity<>(errorResponse, status);
    }
} 