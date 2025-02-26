package com.example.util;

import com.example.model.common.RestRequestModel;
import com.example.model.common.RestResponseModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central REST client class.
 * All REST calls should be made through this class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestClient {
    private final RestTemplate restTemplate;
    private final JsonUtils jsonUtils;

    /**
     * Makes a generic REST call.
     *
     * @param request REST request model
     * @param <T> Response type
     * @return REST response model
     */
    public <T> RestResponseModel<T> execute(RestRequestModel<T> request) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Executing REST request: {}", request);
            
            // Create URI
            URI uri = buildUri(request.getUrl(), request.getQueryParams());
            
            // Prepare headers
            HttpHeaders headers = prepareHeaders(request.getHeaders());
            
            // Prepare body
            Object body = request.getBody();
            if (body != null && !(body instanceof String) && !body.getClass().isPrimitive()) {
                body = jsonUtils.toJson(body);
            }
            
            // Create request entity
            HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);
            
            // Make REST call
            ResponseEntity<T> response;
            if (request.getResponseType() != null) {
                response = restTemplate.exchange(
                        uri,
                        request.getMethod(),
                        requestEntity,
                        request.getResponseType()
                );
            } else {
                // If responseType is not specified, get as String and convert later
                ResponseEntity<String> stringResponse = restTemplate.exchange(
                        uri,
                        request.getMethod(),
                        requestEntity,
                        String.class
                );
                
                // Convert String response to desired type
                T convertedBody = null;
                if (stringResponse.getBody() != null && request.getResponseType() != null) {
                    convertedBody = jsonUtils.fromJson(stringResponse.getBody(), request.getResponseType());
                }
                
                response = new ResponseEntity<>(
                        convertedBody,
                        stringResponse.getHeaders(),
                        stringResponse.getStatusCode()
                );
            }
            
            // Convert headers to Map<String, String> format
            Map<String, String> responseHeaders = response.getHeaders().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> String.join(", ", e.getValue()),
                            (v1, v2) -> v1
                    ));
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("REST request completed in {}ms: {}", duration, response.getStatusCode());
            
            return RestResponseModel.success(
                    response.getStatusCode(),
                    response.getBody(),
                    responseHeaders,
                    duration
            );
            
        } catch (HttpStatusCodeException e) {
            // HTTP error status (4xx, 5xx)
            long duration = System.currentTimeMillis() - startTime;
            log.error("REST request failed with status {}: {}", e.getStatusCode(), e.getMessage());
            
            if (request.isFailOnError()) {
                throw new RuntimeException("REST request failed: " + e.getMessage(), e);
            }
            
            return RestResponseModel.error(
                    e.getStatusCode(),
                    e.getMessage(),
                    e,
                    duration
            );
            
        } catch (ResourceAccessException e) {
            // Connection error
            long duration = System.currentTimeMillis() - startTime;
            log.error("REST connection error: {}", e.getMessage(), e);
            
            if (request.isFailOnError()) {
                throw new RuntimeException("REST connection error: " + e.getMessage(), e);
            }
            
            return RestResponseModel.error(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    e.getMessage(),
                    e,
                    duration
            );
            
        } catch (Exception e) {
            // Other errors
            long duration = System.currentTimeMillis() - startTime;
            log.error("REST request error: {}", e.getMessage(), e);
            
            if (request.isFailOnError()) {
                throw new RuntimeException("REST request error: " + e.getMessage(), e);
            }
            
            return RestResponseModel.error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage(),
                    e,
                    duration
            );
        }
    }
    
    /**
     * Makes a GET request.
     *
     * @param url Endpoint URL
     * @param responseType Response type
     * @param <T> Response type
     * @return REST response model
     */
    public <T> RestResponseModel<T> get(String url, Class<T> responseType) {
        return execute(RestRequestModel.<T>builder()
                .url(url)
                .method(HttpMethod.GET)
                .responseType(responseType)
                .build());
    }
    
    /**
     * Makes a GET request (with query parameters).
     *
     * @param url Endpoint URL
     * @param queryParams Query parameters
     * @param responseType Response type
     * @param <T> Response type
     * @return REST response model
     */
    public <T> RestResponseModel<T> get(String url, Map<String, String> queryParams, Class<T> responseType) {
        return execute(RestRequestModel.<T>builder()
                .url(url)
                .method(HttpMethod.GET)
                .queryParams(convertToMultiValueMap(queryParams))
                .responseType(responseType)
                .build());
    }
    
    /**
     * Makes a POST request.
     *
     * @param url Endpoint URL
     * @param body Request body
     * @param responseType Response type
     * @param <T> Response type
     * @return REST response model
     */
    public <T> RestResponseModel<T> post(String url, Object body, Class<T> responseType) {
        return execute(RestRequestModel.<T>builder()
                .url(url)
                .method(HttpMethod.POST)
                .body(body)
                .responseType(responseType)
                .build());
    }
    
    /**
     * Makes a POST request (with headers).
     *
     * @param url Endpoint URL
     * @param body Request body
     * @param headers Request headers
     * @param responseType Response type
     * @param <T> Response type
     * @return REST response model
     */
    public <T> RestResponseModel<T> post(String url, Object body, Map<String, String> headers, Class<T> responseType) {
        return execute(RestRequestModel.<T>builder()
                .url(url)
                .method(HttpMethod.POST)
                .body(body)
                .headers(headers)
                .responseType(responseType)
                .build());
    }
    
    /**
     * Makes a PUT request.
     *
     * @param url Endpoint URL
     * @param body Request body
     * @param responseType Response type
     * @param <T> Response type
     * @return REST response model
     */
    public <T> RestResponseModel<T> put(String url, Object body, Class<T> responseType) {
        return execute(RestRequestModel.<T>builder()
                .url(url)
                .method(HttpMethod.PUT)
                .body(body)
                .responseType(responseType)
                .build());
    }
    
    /**
     * Makes a DELETE request.
     *
     * @param url Endpoint URL
     * @param responseType Response type
     * @param <T> Response type
     * @return REST response model
     */
    public <T> RestResponseModel<T> delete(String url, Class<T> responseType) {
        return execute(RestRequestModel.<T>builder()
                .url(url)
                .method(HttpMethod.DELETE)
                .responseType(responseType)
                .build());
    }
    
    /**
     * Creates a URI.
     *
     * @param url Base URL
     * @param queryParams Query parameters
     * @return Created URI
     */
    private URI buildUri(String url, org.springframework.util.MultiValueMap<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (queryParams != null) {
            builder.queryParams(queryParams);
        }
        return builder.build().encode().toUri();
    }
    
    /**
     * Prepares HTTP headers.
     *
     * @param headerMap Header map
     * @return HTTP headers
     */
    private HttpHeaders prepareHeaders(Map<String, String> headerMap) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        if (headerMap != null) {
            headerMap.forEach(headers::set);
        }
        
        return headers;
    }
    
    /**
     * Converts Map to MultiValueMap.
     *
     * @param map Map to convert
     * @return MultiValueMap
     */
    private org.springframework.util.MultiValueMap<String, String> convertToMultiValueMap(Map<String, String> map) {
        org.springframework.util.MultiValueMap<String, String> multiValueMap = new org.springframework.util.LinkedMultiValueMap<>();
        if (map != null) {
            map.forEach(multiValueMap::add);
        }
        return multiValueMap;
    }
} 