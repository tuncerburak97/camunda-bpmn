package com.example.handler;

import com.example.model.entity.TaskApiMapping;
import com.example.repository.TaskApiMappingRepository;
import com.example.util.JsonUtils;
import com.example.util.RestClient;
import com.example.model.common.RestRequestModel;
import com.example.model.common.RestResponseModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionService {
    private final TaskApiMappingRepository taskApiMappingRepository;
    private final RestClient restClient;
    private final JsonUtils jsonUtils;

    //@Retryable(maxAttemptsExpression = "#{#taskMapping.maxRetries}",
    //           backoff = @Backoff(delayExpression = "#{#taskMapping.retryTimeout}"))
    public Map<String, Object> executeTask(Long bpmnProcessId, String taskId, Map<String, Object> variables) {
        TaskApiMapping taskMapping;
        if (bpmnProcessId != null) {
            taskMapping = taskApiMappingRepository.findByBpmnProcessIdAndTaskId(bpmnProcessId, taskId)
                    .orElseGet(() -> taskApiMappingRepository.findByTaskId(taskId)
                            .orElseThrow(() -> new RuntimeException("Task mapping not found for taskId: " + taskId)));
        } else {
            taskMapping = taskApiMappingRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new RuntimeException("Task mapping not found for taskId: " + taskId));
        }

        if (!taskMapping.getEnabled()) {
            log.warn("Task mapping is disabled for taskId: {}", taskId);
            return variables;
        }

        log.info("Executing task: {} with variables: {}", taskId, variables);
        
        try {
            // Validate request
            if (taskMapping.getRequestSchema() != null) {
                jsonUtils.validateJsonSchema(processTemplate(taskMapping.getRequestTemplate(), variables), taskMapping.getRequestSchema());
            }

            // Prepare request
            Map<String, String> headers = prepareHeaders(taskMapping.getHeaders(), variables);
            String requestBody = processTemplate(taskMapping.getRequestTemplate(), variables);
            
            log.debug("Prepared request for {}: URL={}, Method={}, Headers={}, Body={}", 
                    taskId, taskMapping.getApiUrl(), taskMapping.getHttpMethod(), headers, requestBody);

            // Execute API call with timeout
            RestRequestModel<String> requestModel = RestRequestModel.<String>builder()
                    .url(taskMapping.getApiUrl())
                    .method(HttpMethod.valueOf(taskMapping.getHttpMethod()))
                    .headers(headers)
                    .body(requestBody)
                    .responseType(String.class)
                    .timeout(Math.toIntExact(taskMapping.getTimeout()))
                    .maxRetries(taskMapping.getMaxRetries())
                    .retryDelay(Math.toIntExact(taskMapping.getRetryTimeout()))
                    .failOnError(taskMapping.getFailOnError())
                    .build();
            
            RestResponseModel<String> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("API call failed: " + response.getErrorMessage());
            }
            
            log.debug("Received response for {}: Status={}, Body={}", 
                    taskId, response.getStatus(), response.getBody());

            // Validate response
            if (taskMapping.getResponseSchema() != null) {
                jsonUtils.validateJsonSchema(response.getBody(), taskMapping.getResponseSchema());
            }

            // Process response
            Map<String, Object> result = new HashMap<>(variables);
            if (taskMapping.getResponseMapping() != null && response.getBody() != null) {
                log.debug("Processing response mapping: {}", taskMapping.getResponseMapping());
                Map<String, Object> mappedResponse = processResponseMapping(response.getBody(), taskMapping.getResponseMapping());
                log.debug("Mapped response: {}", mappedResponse);
                result.putAll(mappedResponse);
            }
            
            log.info("Successfully executed task: {} with result variables: {}", taskId, result);
            return result;

        } catch (Exception e) {
            log.error("Error executing task: {} - {}", taskId, e.getMessage(), e);
            
            if (taskMapping.getFailOnError()) {
                throw new RuntimeException("Task execution failed: " + e.getMessage(), e);
            }

            // Process error mapping if available
            if (taskMapping.getErrorMapping() != null) {
                try {
                    Map<String, Object> errorResult = processErrorMapping(e, taskMapping.getErrorMapping());
                    variables.putAll(errorResult);
                } catch (Exception ex) {
                    log.error("Error processing error mapping", ex);
                }
            }

            return variables;
        }
    }

    private Map<String, String> prepareHeaders(String headerTemplate, Map<String, Object> variables) {
        Map<String, String> headers = new HashMap<>();
        
        if (headerTemplate != null && !headerTemplate.trim().isEmpty()) {
            try {
                // Debug log - header template and variables
                log.debug("Header template before processing: {}", headerTemplate);
                log.debug("Variables for header processing: {}", variables);
                
                // Process template
                String processedTemplate = processTemplate(headerTemplate, variables);
                log.debug("Processed header template: {}", processedTemplate);
                
                // Convert to JSON
                headers = jsonUtils.jsonToStringMap(processedTemplate);
                log.debug("Final headers: {}", headers);
            } catch (Exception e) {
                log.error("Error processing headers: {}", e.getMessage(), e);
            }
        } else {
            log.debug("No header template provided or empty template");
        }

        return headers;
    }

    private String processTemplate(String template, Map<String, Object> variables) {
        if (template == null) return null;
        
        String processed = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            
            // Debug log - variable replacement
            if (processed.contains(placeholder)) {
                log.debug("Replacing placeholder '{}' with value '{}'", placeholder, value);
            }
            
            processed = processed.replace(placeholder, value);
        }
        return processed;
    }

    private Map<String, Object> processResponseMapping(String responseBody, String mappingTemplate) {
        try {
            log.debug("Processing response mapping. Response body: {}, Mapping template: {}", responseBody, mappingTemplate);
            
            // Convert response body to JSON
            Map<String, Object> responseMap = jsonUtils.jsonToMap(responseBody);
            log.debug("Parsed response map: {}", responseMap);
            
            // Convert mapping template to JSON
            Map<String, String> mappings = jsonUtils.jsonToStringMap(mappingTemplate);
            log.debug("Parsed mappings: {}", mappings);
            
            Map<String, Object> result = new HashMap<>();
            
            for (Map.Entry<String, String> mapping : mappings.entrySet()) {
                String targetKey = mapping.getKey();
                String sourcePath = mapping.getValue();
                log.debug("Processing mapping: {} -> {}", sourcePath, targetKey);
                
                // Fix paths starting with "response."
                if (sourcePath.startsWith("response.")) {
                    sourcePath = sourcePath.substring("response.".length());
                }
                
                String[] path = sourcePath.split("\\.");
                Object value = responseMap;
                
                // Follow path to find value
                for (String key : path) {
                    if (value instanceof Map) {
                        value = ((Map) value).get(key);
                        log.debug("Following path '{}', current value: {}", key, value);
                    } else {
                        log.warn("Cannot follow path '{}' as current value is not a map: {}", key, value);
                        value = null;
                        break;
                    }
                }
                
                if (value != null) {
                    log.debug("Setting result variable '{}' to value: {}", targetKey, value);
                    result.put(targetKey, value);
                } else {
                    log.warn("No value found for path '{}', skipping mapping for '{}'", sourcePath, targetKey);
                }
            }
            
            log.debug("Final mapped result: {}", result);
            return result;
        } catch (Exception e) {
            log.error("Error processing response mapping: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing response mapping", e);
        }
    }

    private Map<String, Object> processErrorMapping(Exception error, String errorMapping) {
        try {
            Map<String, String> mappings = jsonUtils.jsonToStringMap(errorMapping);
            Map<String, Object> result = new HashMap<>();
            
            // Create error context
            Map<String, Object> errorContext = new HashMap<>();
            errorContext.put("message", error.getMessage());
            errorContext.put("type", error.getClass().getSimpleName());
            errorContext.put("timestamp", System.currentTimeMillis());
            
            // Apply mappings
            for (Map.Entry<String, String> mapping : mappings.entrySet()) {
                String[] path = mapping.getValue().split("\\.");
                Object value = errorContext;
                
                for (String key : path) {
                    if (value instanceof Map) {
                        value = ((Map) value).get(key);
                    }
                }
                
                if (value != null) {
                    result.put(mapping.getKey(), value);
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("Error processing error mapping", e);
            return Map.of("error", error.getMessage());
        }
    }
} 