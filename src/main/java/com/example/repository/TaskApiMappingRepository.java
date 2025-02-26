package com.example.repository;

import com.example.model.entity.TaskApiMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskApiMappingRepository extends JpaRepository<TaskApiMapping, Long> {
    List<TaskApiMapping> findByBpmnProcessId(Long bpmnProcessId);
    Optional<TaskApiMapping> findByBpmnProcessIdAndTaskId(Long bpmnProcessId, String taskId);
    Optional<TaskApiMapping> findByBpmnProcessIdAndTaskName(Long bpmnProcessId, String taskName);
    Optional<TaskApiMapping> findByTaskId(String taskId);
} 