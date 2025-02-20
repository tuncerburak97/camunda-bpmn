package com.example.controller;

import com.example.model.entity.TaskApiMapping;
import com.example.repository.TaskApiMappingRepository;
import com.example.service.TaskExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/task-mapping")
@RequiredArgsConstructor
public class TaskMappingController {
    private final TaskApiMappingRepository taskApiMappingRepository;
    private final TaskExecutionService taskExecutionService;

    @PostMapping
    public ResponseEntity<TaskApiMapping> createTaskMapping(@RequestBody TaskApiMapping taskMapping) {
        return ResponseEntity.ok(taskApiMappingRepository.save(taskMapping));
    }

    @GetMapping("/process/{processId}")
    public ResponseEntity<List<TaskApiMapping>> getTaskMappings(@PathVariable Long processId) {
        return ResponseEntity.ok(taskApiMappingRepository.findByBpmnProcessId(processId));
    }

    @PostMapping("/execute/{processId}/{taskId}")
    public ResponseEntity<Void> executeTask(
            @PathVariable Long processId,
            @PathVariable String taskId,
            @RequestBody Map<String, Object> variables) {
        try {
            taskExecutionService.executeTask(processId, taskId, variables);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskApiMapping> updateTaskMapping(
            @PathVariable Long id,
            @RequestBody TaskApiMapping taskMapping) {
        if (!taskApiMappingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        taskMapping.setId(id);
        return ResponseEntity.ok(taskApiMappingRepository.save(taskMapping));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTaskMapping(@PathVariable Long id) {
        if (!taskApiMappingRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        taskApiMappingRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
} 