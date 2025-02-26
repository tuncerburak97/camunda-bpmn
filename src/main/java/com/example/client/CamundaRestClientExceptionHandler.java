package com.example.client;

import com.example.exception.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * Exception handler for Camunda REST client
 * Converts REST client exceptions to application-specific exceptions
 */
@Slf4j
public class CamundaRestClientExceptionHandler {

    private static final String CLIENT_NAME = "CamundaRestClient";

    /**
     * Handle exceptions from REST client calls
     * @param ex The exception that occurred
     * @param endpoint The endpoint that was called
     * @throws ClientException Converted client exception
     */
    public static void handleException(Exception ex, String endpoint) {
        log.error("Error calling Camunda REST API at {}: {}", endpoint, ex.getMessage(), ex);
        
        ClientException clientException;
        
        if (ex instanceof HttpClientErrorException) {
            HttpClientErrorException clientEx = (HttpClientErrorException) ex;
            clientException = new ClientException(
                    "Client error: " + clientEx.getStatusCode() + " - " + clientEx.getStatusText(),
                    clientEx.getStatusCode(),
                    CLIENT_NAME,
                    endpoint
            );
        } else if (ex instanceof HttpServerErrorException) {
            HttpServerErrorException serverEx = (HttpServerErrorException) ex;
            clientException = new ClientException(
                    "Server error: " + serverEx.getStatusCode() + " - " + serverEx.getStatusText(),
                    serverEx.getStatusCode(),
                    CLIENT_NAME,
                    endpoint
            );
        } else if (ex instanceof ResourceAccessException) {
            clientException = new ClientException(
                    "Service unavailable: " + ex.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE,
                    CLIENT_NAME,
                    endpoint
            );
        } else if (ex instanceof RestClientException) {
            clientException = new ClientException(
                    "REST client error: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    CLIENT_NAME,
                    endpoint
            );
        } else {
            clientException = new ClientException(
                    "Unexpected error: " + ex.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    CLIENT_NAME,
                    endpoint
            );
        }
        
        // Set the original exception as the cause
        clientException.initCause(ex);
        
        throw clientException;
    }
} 