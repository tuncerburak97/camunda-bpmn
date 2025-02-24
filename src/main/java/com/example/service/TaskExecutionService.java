package com.example.service;

import com.example.model.entity.TaskApiMapping;
import com.example.repository.TaskApiMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutionService {
    private final TaskApiMappingRepository taskApiMappingRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public void executeTask(Long bpmnProcessId, String taskId, Map<String, Object> variables) {
        TaskApiMapping taskMapping = taskApiMappingRepository.findByBpmnProcessIdAndTaskId(bpmnProcessId, taskId)
                .orElseThrow(() -> new RuntimeException("Task mapping not found for processId: " + bpmnProcessId + " and taskId: " + taskId));

        // Prepare request
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // Process request template with variables
        String requestBody = processTemplate(taskMapping.getRequestTemplate(), variables);

        // Execute API call
        ResponseEntity<String> response = restTemplate.exchange(
                taskMapping.getApiUrl(),
                HttpMethod.valueOf(taskMapping.getHttpMethod()),
                new HttpEntity<>(requestBody, headers),
                String.class
        );

        // Process response mapping if needed
        if (taskMapping.getResponseMapping() != null) {
            processResponseMapping(response.getBody(), taskMapping.getResponseMapping(), variables);
        }
    }

    private String processTemplate(String template, Map<String, Object> variables) {
        if (template == null) return null;
        
        // Simple template processing - replace ${varName} with actual values
        String processed = template;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            processed = processed.replace("${" + entry.getKey() + "}", 
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return processed;
    }

    private void processResponseMapping(String responseBody, String mappingTemplate, Map<String, Object> variables) {
        try {
            // Convert response to map
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            
            // Process mapping template and update variables
            // This is a simplified version - you might want to implement more sophisticated mapping
            Map<String, String> mappings = objectMapper.readValue(mappingTemplate, Map.class);
            
            for (Map.Entry<String, String> mapping : mappings.entrySet()) {
                String[] path = mapping.getValue().split("\\.");
                Object value = responseMap;
                
                for (String key : path) {
                    if (value instanceof Map) {
                        value = ((Map) value).get(key);
                    }
                }
                
                if (value != null) {
                    variables.put(mapping.getKey(), value);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing response mapping", e);
        }
    }

    public Map<String, Object> executeLogin(Map<String, Object> variables) {
        log.info("Executing login with variables: {}", variables);
        
        // Burada gerçek login işlemi yapılacak
        // Örnek olarak başarılı login simüle ediyoruz
        Map<String, Object> result = new HashMap<>();
        result.put("token", "sample-token-" + System.currentTimeMillis());
        result.put("loginSuccess", true);
        
        log.info("Login completed with result: {}", result);
        return result;
    }

    public Map<String, Object> executeGetProfile(Map<String, Object> variables) {
        log.info("Executing get profile with variables: {}", variables);
        
        // Burada gerçek profil bilgisi çekme işlemi yapılacak
        // Örnek olarak profil bilgisi simüle ediyoruz
        Map<String, Object> result = new HashMap<>();
        result.put("email", "user@example.com");
        result.put("name", "Test User");
        result.put("profileFound", true);
        
        log.info("Get profile completed with result: {}", result);
        return result;
    }
} 