package com.example.service;

import com.example.model.entity.TaskApiMapping;
import com.example.model.entity.BpmnProcess;
import com.example.repository.TaskApiMappingRepository;
import com.example.repository.BpmnProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskMappingService {
    private final TaskApiMappingRepository taskApiMappingRepository;
    private final BpmnProcessRepository bpmnProcessRepository;

    @Transactional
    public TaskApiMapping createTaskMapping(TaskApiMapping taskMapping) {
        BpmnProcess bpmnProcess = bpmnProcessRepository.findById(taskMapping.getBpmnProcess().getId())
                .orElseThrow(() -> new EntityNotFoundException("BPMN process not found with id: " + taskMapping.getBpmnProcess().getId()));
        taskMapping.setBpmnProcess(bpmnProcess);
        return taskApiMappingRepository.save(taskMapping);
    }

    @Transactional(readOnly = true)
    public List<TaskApiMapping> getTaskMappingsByProcessId(Long processId) {
        return taskApiMappingRepository.findByBpmnProcessId(processId);
    }

    @Transactional
    public TaskApiMapping updateTaskMapping(Long id, TaskApiMapping taskMapping) {
        if (!taskApiMappingRepository.existsById(id)) {
            throw new EntityNotFoundException("Task mapping not found with id: " + id);
        }
        taskMapping.setId(id);
        return taskApiMappingRepository.save(taskMapping);
    }

    @Transactional
    public void deleteTaskMapping(Long id) {
        if (!taskApiMappingRepository.existsById(id)) {
            throw new EntityNotFoundException("Task mapping not found with id: " + id);
        }
        taskApiMappingRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public TaskApiMapping getTaskMappingById(Long id) {
        return taskApiMappingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task mapping not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public TaskApiMapping findByBpmnProcessIdAndTaskId(Long processId, String taskId) {
        return taskApiMappingRepository.findByBpmnProcessIdAndTaskId(processId, taskId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Task mapping not found for processId: %d and taskId: %s", processId, taskId)));
    }
} 
