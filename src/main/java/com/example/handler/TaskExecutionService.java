package com.example.handler;

import com.example.model.entity.TaskApiMapping;
import com.example.repository.TaskApiMappingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionService {
    private final TaskApiMappingRepository taskApiMappingRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

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
                validateJson(processTemplate(taskMapping.getRequestTemplate(), variables), taskMapping.getRequestSchema());
            }

            // Prepare request
            HttpHeaders headers = prepareHeaders(taskMapping.getHeaders(), variables);
            String requestBody = processTemplate(taskMapping.getRequestTemplate(), variables);
            
            log.debug("Prepared request for {}: URL={}, Method={}, Headers={}, Body={}", 
                    taskId, taskMapping.getApiUrl(), taskMapping.getHttpMethod(), headers, requestBody);

            // Execute API call with timeout
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    taskMapping.getApiUrl(),
                    HttpMethod.valueOf(taskMapping.getHttpMethod()),
                    requestEntity,
                    String.class
            );
            
            log.debug("Received response for {}: Status={}, Body={}", 
                    taskId, response.getStatusCode(), response.getBody());

            // Validate response
            if (taskMapping.getResponseSchema() != null) {
                validateJson(response.getBody(), taskMapping.getResponseSchema());
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

    private HttpHeaders prepareHeaders(String headerTemplate, Map<String, Object> variables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (headerTemplate != null && !headerTemplate.trim().isEmpty()) {
            try {
                // Debug log - header template ve değişkenler
                log.debug("Header template before processing: {}", headerTemplate);
                log.debug("Variables for header processing: {}", variables);
                
                // Template'i işle
                String processedTemplate = processTemplate(headerTemplate, variables);
                log.debug("Processed header template: {}", processedTemplate);
                
                // JSON'a çevir
                Map<String, String> headerMap = objectMapper.readValue(
                    processedTemplate,
                    Map.class
                );
                
                // Her bir header'ı ekle
                headerMap.forEach((key, value) -> {
                    log.debug("Adding header: {} = {}", key, value);
                    headers.set(key, value);
                });
                
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
            
            // Debug log - değişken değiştirme
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
            
            // Response body'yi JSON'a çevir
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            log.debug("Parsed response map: {}", responseMap);
            
            // Mapping template'i JSON'a çevir
            Map<String, String> mappings = objectMapper.readValue(mappingTemplate, Map.class);
            log.debug("Parsed mappings: {}", mappings);
            
            Map<String, Object> result = new HashMap<>();
            
            for (Map.Entry<String, String> mapping : mappings.entrySet()) {
                String targetKey = mapping.getKey();
                String sourcePath = mapping.getValue();
                log.debug("Processing mapping: {} -> {}", sourcePath, targetKey);
                
                // "response." ile başlayan path'leri düzelt
                if (sourcePath.startsWith("response.")) {
                    sourcePath = sourcePath.substring("response.".length());
                }
                
                String[] path = sourcePath.split("\\.");
                Object value = responseMap;
                
                // Path'i takip ederek değeri bul
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
            Map<String, String> mappings = objectMapper.readValue(errorMapping, Map.class);
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

    private void validateJson(String json, String schema) {
        try {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
            JsonSchema jsonSchema = factory.getSchema(schema);
            JsonNode jsonNode = objectMapper.readTree(json);
            
            Set<ValidationMessage> validationResult = jsonSchema.validate(jsonNode);
            if (!validationResult.isEmpty()) {
                throw new RuntimeException("JSON validation failed: " + validationResult);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error validating JSON", e);
        }
    }
} 