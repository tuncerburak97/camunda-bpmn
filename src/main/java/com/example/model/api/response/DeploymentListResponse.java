package com.example.model.api.response;

import com.example.model.client.DeploymentResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * API response model for deployment list endpoint
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeploymentListResponse {
    private List<DeploymentResponse> deployments;
    private Integer totalCount;
    private String message;
} 