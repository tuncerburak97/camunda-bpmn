package com.example.client;

import com.example.model.client.CreateDeploymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.util.List;
import java.util.Map;
import com.example.model.client.DeploymentResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class CamundaRestClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${camunda.rest.url:http://localhost:8080/engine-rest}")
    private String camundaRestUrl;

    /**
     * Deploy BPMN process
     */
    public void deployProcess(String deploymentName, File bpmnFile) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        // Required fields
        body.add("deployment-name", deploymentName);
        
        // Optional fields
        body.add("enable-duplicate-filtering", "true");
        body.add("deployment-source", "rest-api");
        
        // Add binary file
        FileSystemResource fileResource = new FileSystemResource(bpmnFile);
        body.add("data", fileResource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            camundaRestUrl + "/deployment/create",
            HttpMethod.POST,
            requestEntity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("Deploy error: {}", response.getBody());
            throw new RuntimeException("Process not deployed to Camunda: " + response.getBody());
        }
        
        log.info("Process deployed successfully: {}", deploymentName);
    }

    /**
     * Start process instance
     */
    public String startProcess(String processKey, Map<String, Object> variables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
            "variables", variables
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            camundaRestUrl + "/process-definition/key/" + processKey + "/start",
            HttpMethod.POST,
            requestEntity,
            Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Process not started: " + response.getBody());
        }

        return (String) response.getBody().get("id");
    }

    /**
     * Task'ı tamamlar
     */
    public void completeTask(String taskId, Map<String, Object> variables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
            "variables", variables
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response = restTemplate.exchange(
            camundaRestUrl + "/task/" + taskId + "/complete",
            HttpMethod.POST,
            requestEntity,
            Void.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Task not completed: " + taskId);
        }
    }

    /**
     * Process instance'ını siler
     */
    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        ResponseEntity<Void> response = restTemplate.exchange(
            camundaRestUrl + "/process-instance/" + processInstanceId + "?skipCustomListeners=true&skipIoMappings=true",
            HttpMethod.DELETE,
            null,
            Void.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Process instance not deleted: " + processInstanceId);
        }
    }

    /**
     * Deployment'ı siler
     */
    public void deleteDeployment(String deploymentId, boolean cascade) {
        ResponseEntity<Void> response = restTemplate.exchange(
            camundaRestUrl + "/deployment/" + deploymentId + "?cascade=" + cascade,
            HttpMethod.DELETE,
            null,
            Void.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Deployment not deleted: " + deploymentId);
        }
    }

    /**
     * Process instance'a ait task'ları getirir
     */
    public List<Map<String, Object>> getTasksByProcessInstanceId(String processInstanceId) {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            camundaRestUrl + "/task?processInstanceId=" + processInstanceId,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Tasks not retrieved: " + processInstanceId);
        }

        return response.getBody();
    }

    /**
     * Task detaylarını getirir
     */
    public Map<String, Object> getTask(String taskId) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            camundaRestUrl + "/task/" + taskId,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Task details not retrieved: " + taskId);
        }

        return response.getBody();
    }

    /**
     * Process instance değişkenlerini getirir
     */
    public Map<String, Object> getProcessVariables(String processInstanceId) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            camundaRestUrl + "/process-instance/" + processInstanceId + "/variables",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Process variables not retrieved: " + processInstanceId);
        }

        return response.getBody();
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
        StringBuilder urlBuilder = new StringBuilder(camundaRestUrl + "/deployment?");
        
        if (id != null) urlBuilder.append("id=").append(id).append("&");
        if (name != null) urlBuilder.append("name=").append(name).append("&");
        if (nameLike != null) urlBuilder.append("nameLike=").append(nameLike).append("&");
        if (source != null) urlBuilder.append("source=").append(source).append("&");
        if (withoutSource != null) urlBuilder.append("withoutSource=").append(withoutSource).append("&");
        if (tenantIdIn != null) urlBuilder.append("tenantIdIn=").append(tenantIdIn).append("&");
        if (withoutTenantId != null) urlBuilder.append("withoutTenantId=").append(withoutTenantId).append("&");
        if (includeDeploymentsWithoutTenantId != null) urlBuilder.append("includeDeploymentsWithoutTenantId=").append(includeDeploymentsWithoutTenantId).append("&");
        if (after != null) urlBuilder.append("after=").append(after).append("&");
        if (before != null) urlBuilder.append("before=").append(before).append("&");
        if (sortBy != null) urlBuilder.append("sortBy=").append(sortBy).append("&");
        if (sortOrder != null) urlBuilder.append("sortOrder=").append(sortOrder).append("&");
        if (firstResult != null) urlBuilder.append("firstResult=").append(firstResult).append("&");
        if (maxResults != null) urlBuilder.append("maxResults=").append(maxResults).append("&");

        ResponseEntity<List<DeploymentResponse>> response = restTemplate.exchange(
            urlBuilder.toString(),
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<DeploymentResponse>>() {}
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Error retrieving deployments");
        }

        return response.getBody();
    }
} 