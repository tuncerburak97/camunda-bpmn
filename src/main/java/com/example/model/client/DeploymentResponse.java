package com.example.model.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Represents a deployment response from Camunda API
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeploymentResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("tenantId")
    private String tenantId;
    
    @JsonProperty("deploymentTime")
    private OffsetDateTime deploymentTime;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("links")
    private List<AtomLink> links;
} 