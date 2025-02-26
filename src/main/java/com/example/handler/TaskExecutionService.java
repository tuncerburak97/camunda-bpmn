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

        try {
            // Validate request
            if (taskMapping.getRequestSchema() != null) {
                validateJson(processTemplate(taskMapping.getRequestTemplate(), variables), taskMapping.getRequestSchema());
            }

            // Prepare request
            HttpHeaders headers = prepareHeaders(taskMapping.getHeaders(), variables);
            String requestBody = processTemplate(taskMapping.getRequestTemplate(), variables);

            // Execute API call with timeout
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    taskMapping.getApiUrl(),
                    HttpMethod.valueOf(taskMapping.getHttpMethod()),
                    requestEntity,
                    String.class
            );

            // Validate response
            if (taskMapping.getResponseSchema() != null) {
                validateJson(response.getBody(), taskMapping.getResponseSchema());
            }

            // Process response
            Map<String, Object> result = new HashMap<>(variables);
            if (taskMapping.getResponseMapping() != null) {
                Map<String, Object> mappedResponse = processResponseMapping(response.getBody(), taskMapping.getResponseMapping());
                result.putAll(mappedResponse);
            }

            return result;

        } catch (Exception e) {
            log.error("Error executing task: {} - {}", taskId, e.getMessage());
            
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

        if (headerTemplate != null) {
            try {
                Map<String, String> headerMap = objectMapper.readValue(
                    processTemplate(headerTemplate, variables),
                    Map.class
                );
                headerMap.forEach(headers::set);
            } catch (Exception e) {
                log.error("Error processing headers", e);
            }
        }

        return headers;
    }

    private String processTemplate(String template, Map<String, Object> variables) {
        if (template == null) return null;
        
        String processed = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            processed = processed.replace("${" + entry.getKey() + "}", 
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return processed;
    }

    private Map<String, Object> processResponseMapping(String responseBody, String mappingTemplate) {
        try {
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            Map<String, String> mappings = objectMapper.readValue(mappingTemplate, Map.class);
            Map<String, Object> result = new HashMap<>();
            
            for (Map.Entry<String, String> mapping : mappings.entrySet()) {
                String[] path = mapping.getValue().split("\\.");
                Object value = responseMap;
                
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