package com.example.model.client;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateDeploymentResponse {
    private List<AtomLink> links;
    private String id;
    private String name;
    private String source;
    private LocalDateTime deploymentTime;
    private String tenantId;
    private Object deployedProcessDefinitions;
    private Object deployedCaseDefinitions;
    private Object deployedDecisionDefinitions;
    private Object deployedDecisionRequirementsDefinitions;
}
