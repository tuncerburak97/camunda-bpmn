package com.example.controller;

import com.example.model.entity.BpmnProcess;
import com.example.service.BpmnDeploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/bpmn")
@RequiredArgsConstructor
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
} 