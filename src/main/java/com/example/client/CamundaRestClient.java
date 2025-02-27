package com.example.client;

import com.example.exception.ClientException;
import com.example.model.client.DeploymentResponse;
import com.example.model.common.RestRequestModel;
import com.example.model.common.RestResponseModel;
import com.example.util.JsonUtils;
import com.example.util.RestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Camunda REST API client.
 * All Camunda REST API calls should be made through this class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CamundaRestClient {
    
    private final RestClient restClient;
    private final RestTemplate restTemplate;
    private final JsonUtils jsonUtils;
    
    @Value("${camunda.rest.url:http://localhost:8080/engine-rest}")
    private String camundaRestUrl;
    
    private static final String CLIENT_NAME = "CamundaRestClient";

    /**
     * Deploy BPMN process
     */
    public void deployProcess(String deploymentName, File bpmnFile) {
        String endpoint = "/deployment/create";
        try {
            // Special handling for multipart form data
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Required fields
            body.add("deployment-name", deploymentName);
            
            // Optional fields
            body.add("enable-duplicate-filtering", "true");
            body.add("deployment-source", "rest-api");
            
            // Add binary file
            FileSystemResource fileResource = new FileSystemResource(bpmnFile);
            body.add("data", fileResource);
            
            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Create request entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Use RestTemplate directly for multipart requests to avoid JSON serialization issues
            ResponseEntity<String> response = restTemplate.exchange(
                    camundaRestUrl + endpoint,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Deploy error: {}", response.getBody());
                throw new RuntimeException("Process not deployed to Camunda: " + response.getStatusCode());
            }
            
            log.info("Process deployed successfully: {}", deploymentName);
        } catch (Exception ex) {
            handleException(ex, endpoint);
        }
    }

    /**
     * Start process instance
     */
    public String startProcess(String processKey, Map<String, Object> variables) {
        String endpoint = "/process-definition/key/" + processKey + "/start";
        try {
            // Format variables for Camunda
            Map<String, Object> formattedVariables = formatCamundaVariables(variables);
            
            Map<String, Object> body = Map.of(
                "variables", formattedVariables
            );
            
            // Create RestRequestModel
            RestRequestModel<Map> requestModel = RestRequestModel.<Map>builder()
                    .url(camundaRestUrl + endpoint)
                    .method(HttpMethod.POST)
                    .body(body)
                    .responseType(Map.class)
                    .build();
            
            // Send request
            RestResponseModel<Map> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Process not started: " + response.getErrorMessage());
            }

            return (String) response.getBody().get("id");
        } catch (Exception ex) {
            handleException(ex, endpoint);
            return null; // This line will never be reached as handleException always throws an exception
        }
    }

    /**
     * Complete a task
     */
    public void completeTask(String taskId, Map<String, Object> variables) {
        String endpoint = "/task/" + taskId + "/complete";
        try {
            // Format variables for Camunda
            Map<String, Object> formattedVariables = formatCamundaVariables(variables);
            
            Map<String, Object> body = Map.of(
                "variables", formattedVariables
            );
            
            // Create RestRequestModel
            RestRequestModel<Void> requestModel = RestRequestModel.<Void>builder()
                    .url(camundaRestUrl + endpoint)
                    .method(HttpMethod.POST)
                    .body(body)
                    .responseType(Void.class)
                    .build();
            
            // Send request
            RestResponseModel<Void> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Task not completed: " + taskId);
            }
        } catch (Exception ex) {
            handleException(ex, endpoint);
        }
    }

    /**
     * Delete a process instance
     */
    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        String endpoint = "/process-instance/" + processInstanceId;
        try {
            // Query parameters
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add("skipCustomListeners", "true");
            queryParams.add("skipIoMappings", "true");
            
            // Create RestRequestModel
            RestRequestModel<Void> requestModel = RestRequestModel.<Void>builder()
                    .url(camundaRestUrl + endpoint)
                    .method(HttpMethod.DELETE)
                    .queryParams(queryParams)
                    .responseType(Void.class)
                    .build();
            
            // Send request
            RestResponseModel<Void> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Process instance not deleted: " + processInstanceId);
            }
        } catch (Exception ex) {
            handleException(ex, endpoint);
        }
    }

    /**
     * Delete a deployment
     */
    public void deleteDeployment(String deploymentId, boolean cascade) {
        String endpoint = "/deployment/" + deploymentId;
        try {
            // Query parameters
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add("cascade", String.valueOf(cascade));
            
            // Create RestRequestModel
            RestRequestModel<Void> requestModel = RestRequestModel.<Void>builder()
                    .url(camundaRestUrl + endpoint)
                    .method(HttpMethod.DELETE)
                    .queryParams(queryParams)
                    .responseType(Void.class)
                    .build();
            
            // Send request
            RestResponseModel<Void> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Deployment not deleted: " + deploymentId);
            }
        } catch (Exception ex) {
            handleException(ex, endpoint);
        }
    }

    /**
     * Get tasks by process instance ID
     */
    public List<Map<String, Object>> getTasksByProcessInstanceId(String processInstanceId) {
        String endpoint = "/task";
        try {
            // Query parameters
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add("processInstanceId", processInstanceId);
            
            // Create RestRequestModel
            RestRequestModel<List> requestModel = RestRequestModel.<List>builder()
                    .url(camundaRestUrl + endpoint)
                    .method(HttpMethod.GET)
                    .queryParams(queryParams)
                    .responseType(List.class)
                    .build();
            
            // Send request
            RestResponseModel<List> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Tasks not retrieved: " + processInstanceId);
            }
            
            return response.getBody();
        } catch (Exception ex) {
            handleException(ex, endpoint);
            return null; // This line will never be reached as handleException always throws an exception
        }
    }

    /**
     * Get task details
     */
    public Map<String, Object> getTask(String taskId) {
        String endpoint = "/task/" + taskId;
        try {
            // Create RestRequestModel
            RestRequestModel<Map> requestModel = RestRequestModel.<Map>builder()
                    .url(camundaRestUrl + endpoint)
                    .method(HttpMethod.GET)
                    .responseType(Map.class)
                    .build();
            
            // Send request
            RestResponseModel<Map> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Task details not retrieved: " + taskId);
            }
            
            return response.getBody();
        } catch (Exception ex) {
            handleException(ex, endpoint);
            return null; // This line will never be reached as handleException always throws an exception
        }
    }

    /**
     * Get process instance variables
     */
    public Map<String, Object> getProcessVariables(String processInstanceId) {
        String endpoint = "/process-instance/" + processInstanceId + "/variables";
        try {
            // Create RestRequestModel
            RestRequestModel<Map> requestModel = RestRequestModel.<Map>builder()
                    .url(camundaRestUrl + endpoint)
                    .method(HttpMethod.GET)
                    .responseType(Map.class)
                    .build();
            
            // Send request
            RestResponseModel<Map> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Process variables not retrieved: " + processInstanceId);
            }
            
            return response.getBody();
        } catch (Exception ex) {
            handleException(ex, endpoint);
            return null; // This line will never be reached as handleException always throws an exception
        }
    }

    /**
     * Get list of deployments based on various filter criteria
     */
    public List<DeploymentResponse> getDeployments(
            String id,
            String name,
            String nameLike,
            String source,
            Boolean withoutSource,
            String tenantIdIn,
            Boolean withoutTenantId,
            Boolean includeDeploymentsWithoutTenantId,
            String after,
            String before,
            String sortBy,
            String sortOrder,
            Integer firstResult,
            Integer maxResults
    ) {
        String endpoint = "/deployment";
        try {
            // Query parameters
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            if (id != null) queryParams.add("id", id);
            if (name != null) queryParams.add("name", name);
            if (nameLike != null) queryParams.add("nameLike", nameLike);
            if (source != null) queryParams.add("source", source);
            if (withoutSource != null) queryParams.add("withoutSource", withoutSource.toString());
            if (tenantIdIn != null) queryParams.add("tenantIdIn", tenantIdIn);
            if (withoutTenantId != null) queryParams.add("withoutTenantId", withoutTenantId.toString());
            if (includeDeploymentsWithoutTenantId != null) queryParams.add("includeDeploymentsWithoutTenantId", includeDeploymentsWithoutTenantId.toString());
            if (after != null) queryParams.add("after", after);
            if (before != null) queryParams.add("before", before);
            if (sortBy != null) queryParams.add("sortBy", sortBy);
            if (sortOrder != null) queryParams.add("sortOrder", sortOrder);
            if (firstResult != null) queryParams.add("firstResult", firstResult.toString());
            if (maxResults != null) queryParams.add("maxResults", maxResults.toString());
            
            // Create RestRequestModel
            RestRequestModel<List> requestModel = RestRequestModel.<List>builder()
                    .url(camundaRestUrl + endpoint)
                    .method(HttpMethod.GET)
                    .queryParams(queryParams)
                    .responseType(List.class)
                    .build();
            
            // Send request
            RestResponseModel<List> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Deployments not retrieved");
            }
            
            // Convert List<Map> to List<DeploymentResponse>
            List<Map<String, Object>> responseList = response.getBody();
            return responseList.stream()
                    .map(map -> jsonUtils.convert(map, DeploymentResponse.class))
                    .toList();
        } catch (Exception ex) {
            handleException(ex, endpoint);
            return null; // This line will never be reached as handleException always throws an exception
        }
    }

    /**
     * Get external tasks by process instance ID
     */
    public List<Map<String, Object>> getExternalTasksByProcessInstanceId(String processInstanceId) {
        String endpoint = "/external-task";
        try {
            // Query parameters
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add("processInstanceId", processInstanceId);
            
            // Create RestRequestModel
            RestRequestModel<List> requestModel = RestRequestModel.<List>builder()
                    .url(camundaRestUrl + endpoint)
                    .method(HttpMethod.GET)
                    .queryParams(queryParams)
                    .responseType(List.class)
                    .build();
            
            // Send request
            RestResponseModel<List> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("External tasks not retrieved: " + processInstanceId);
            }
            
            return response.getBody();
        } catch (Exception ex) {
            handleException(ex, endpoint);
            return null; // This line will never be reached as handleException always throws an exception
        }
    }
    
    /**
     * Format Camunda variables
     */
    private Map<String, Object> formatCamundaVariables(Map<String, Object> variables) {
        Map<String, Object> formattedVariables = new HashMap<>();
        variables.forEach((key, value) -> {
            Map<String, Object> variableInfo = new HashMap<>();
            variableInfo.put("value", value);
            // Determine type based on value
            String type = determineType(value);
            variableInfo.put("type", type);
            formattedVariables.put(key, variableInfo);
        });
        return formattedVariables;
    }
    
    /**
     * Determine variable type
     */
    private String determineType(Object value) {
        if (value instanceof String) return "String";
        if (value instanceof Integer) return "Integer";
        if (value instanceof Long) return "Long";
        if (value instanceof Double) return "Double";
        if (value instanceof Boolean) return "Boolean";
        if (value instanceof Map) return "Json";
        if (value instanceof List) return "Json";
        return "String"; // default type
    }
    
    /**
     * Handle exceptions
     */
    private void handleException(Exception ex, String endpoint) {
        log.error("Error calling Camunda REST API at {}: {}", endpoint, ex.getMessage(), ex);
        
        if (ex instanceof ClientException) {
            throw (ClientException) ex;
        }
        
        throw new ClientException(
                "Camunda REST API error: " + ex.getMessage(),
                null,
                CLIENT_NAME,
                endpoint,
                ex
        );
    }

    /**
     * Get activity instances for a process instance
     */
    public Map<String, Object> getActivityInstances(String processInstanceId) {
        String endpoint = "/process-instance/" + processInstanceId + "/activity-instances";
        try {
            // Create RestRequestModel
            RestRequestModel<Map> requestModel = RestRequestModel.<Map>builder()
                    .url(camundaRestUrl + endpoint)
                    .method(HttpMethod.GET)
                    .responseType(Map.class)
                    .build();
            
            // Send request
            RestResponseModel<Map> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Activity instances not retrieved: " + processInstanceId);
            }
            
            return response.getBody();
        } catch (Exception ex) {
            handleException(ex, endpoint);
            return null; // This line will never be reached as handleException always throws an exception
        }
    }
    
    /**
     * Modify process instance execution state
     * This can be used to control gateways by starting execution at specific activities
     */
    public void modifyProcessInstance(String processInstanceId, List<Map<String, Object>> instructions) {
        String endpoint = "/process-instance/" + processInstanceId + "/modification";
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("skipCustomListeners", true);
            body.put("skipIoMappings", true);
            body.put("instructions", instructions);
            
            // Create RestRequestModel
            RestRequestModel<Void> requestModel = RestRequestModel.<Void>builder()
                    .url(camundaRestUrl + endpoint)
                    .method(HttpMethod.POST)
                    .body(body)
                    .responseType(Void.class)
                    .build();
            
            // Send request
            RestResponseModel<Void> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Process instance not modified: " + processInstanceId);
            }
            
            log.info("Successfully modified process instance: {}", processInstanceId);
        } catch (Exception ex) {
            handleException(ex, endpoint);
        }
    }
    
    /**
     * Get BPMN model XML for a process definition
     */
    public String getProcessDefinitionXml(String processDefinitionId) {
        String endpoint = "/process-definition/" + processDefinitionId + "/xml";
        try {
            // Create RestRequestModel
            RestRequestModel<Map> requestModel = RestRequestModel.<Map>builder()
                    .url(camundaRestUrl + endpoint)
                    .method(HttpMethod.GET)
                    .responseType(Map.class)
                    .build();
            
            // Send request
            RestResponseModel<Map> response = restClient.execute(requestModel);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("Process definition XML not retrieved: " + processDefinitionId);
            }
            
            return (String) response.getBody().get("bpmn20Xml");
        } catch (Exception ex) {
            handleException(ex, endpoint);
            return null; // This line will never be reached as handleException always throws an exception
        }
    }
} 