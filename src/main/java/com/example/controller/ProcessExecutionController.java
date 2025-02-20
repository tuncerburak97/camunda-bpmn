package com.example.controller;

import com.example.service.ProcessExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process")
@RequiredArgsConstructor
public class ProcessExecutionController {
    private final ProcessExecutionService processExecutionService;

    @PostMapping("/start/{processKey}")
    public ResponseEntity<String> startProcess(
            @PathVariable String processKey,
            @RequestBody(required = false) Map<String, Object> variables) {
        try {
            String processInstanceId = processExecutionService.startProcess(
                    processKey,
                    variables != null ? variables : Map.of()
            );
            return ResponseEntity.ok(processInstanceId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/task/{taskId}/execute")
    public ResponseEntity<Void> executeTask(@PathVariable String taskId) {
        try {
            processExecutionService.executeTask(taskId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/instance/{processInstanceId}/tasks")
    public ResponseEntity<List<Map<String, Object>>> getActiveTasks(@PathVariable String processInstanceId) {
        try {
            List<Map<String, Object>> tasks = processExecutionService.getActiveTasks(processInstanceId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 