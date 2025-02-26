package com.example.controller;

import com.example.model.entity.TaskApiMapping;
import com.example.service.TaskMappingService;
import com.example.handler.TaskExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/task-mapping")
@RequiredArgsConstructor
public class TaskMappingController {
    private final TaskMappingService taskMappingService;
    private final TaskExecutionService taskExecutionService;

    @PostMapping
    public ResponseEntity<TaskApiMapping> createTaskMapping(@RequestBody TaskApiMapping taskMapping) {
        try {
            return ResponseEntity.ok(taskMappingService.createTaskMapping(taskMapping));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/process/{processId}")
    public ResponseEntity<List<TaskApiMapping>> getTaskMappings(@PathVariable Long processId) {
        try {
            return ResponseEntity.ok(taskMappingService.getTaskMappingsByProcessId(processId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/execute/{processId}/{taskId}")
    public ResponseEntity<Void> executeTask(
            @PathVariable Long processId,
            @PathVariable String taskId,
            @RequestBody Map<String, Object> variables) {
        try {
            taskExecutionService.executeTask(processId, taskId, variables);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskApiMapping> updateTaskMapping(
            @PathVariable Long id,
            @RequestBody TaskApiMapping taskMapping) {
        try {
            return ResponseEntity.ok(taskMappingService.updateTaskMapping(id, taskMapping));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTaskMapping(@PathVariable Long id) {
        try {
            taskMappingService.deleteTaskMapping(id);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 