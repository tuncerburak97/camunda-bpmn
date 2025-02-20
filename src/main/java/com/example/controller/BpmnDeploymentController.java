package com.example.controller;

import com.example.model.api.response.DeploymentListResponse;
import com.example.model.client.DeploymentResponse;
import com.example.model.entity.BpmnProcess;
import com.example.service.BpmnDeploymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/bpmn")
@RequiredArgsConstructor
@Slf4j
public class BpmnDeploymentController {
    private final BpmnDeploymentService bpmnDeploymentService;

    @PostMapping("/deploy")
    public ResponseEntity<BpmnProcess> deployBpmnProcess(
            @RequestParam("processName") String processName,
            @RequestParam("processKey") String processKey,
            @RequestParam("bpmnFile") MultipartFile bpmnFile,
            @RequestParam(value = "description", required = false) String description) {
        try {
            BpmnProcess deployedProcess = bpmnDeploymentService.deployBpmnProcess(
                    processName, processKey, bpmnFile, description);
            return ResponseEntity.ok(deployedProcess);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/processes")
    public ResponseEntity<List<BpmnProcess>> getAllBpmnProcesses() {
        try {
            List<BpmnProcess> processes = bpmnDeploymentService.getAllBpmnProcesses();
            return ResponseEntity.ok(processes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/process/{processKey}")
    public ResponseEntity<BpmnProcess> getBpmnProcess(@PathVariable String processKey) {
        try {
            BpmnProcess process = bpmnDeploymentService.getBpmnProcessByKey(processKey);
            return ResponseEntity.ok(process);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/deploy")
    public ResponseEntity<DeploymentListResponse> getDeployments(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String nameLike,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Boolean withoutSource,
            @RequestParam(required = false) String tenantIdIn,
            @RequestParam(required = false) Boolean withoutTenantId,
            @RequestParam(required = false) Boolean includeDeploymentsWithoutTenantId,
            @RequestParam(required = false) String after,
            @RequestParam(required = false) String before,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) Integer firstResult,
            @RequestParam(required = false) Integer maxResults
    ) {
        try {
            List<DeploymentResponse> deployments = bpmnDeploymentService.getDeployments(
                    id, name, nameLike, source, withoutSource, tenantIdIn,
                    withoutTenantId, includeDeploymentsWithoutTenantId,
                    after, before, sortBy, sortOrder, firstResult, maxResults
            );

            return ResponseEntity.ok(DeploymentListResponse.builder()
                    .deployments(deployments)
                    .totalCount(deployments.size())
                    .message("Deployments retrieved successfully")
                    .build());
        } catch (Exception e) {
            log.error("Error retrieving deployments", e);
            return ResponseEntity.ok(DeploymentListResponse.builder()
                    .message("Error retrieving deployments: " + e.getMessage())
                    .build());
        }
    }
} 