package com.example.service;

import com.example.model.entity.BpmnProcess;
import com.example.model.entity.TaskApiMapping;
import com.example.repository.TaskApiMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessExecutionService {
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final BpmnDeploymentService bpmnDeploymentService;
    private final TaskApiMappingRepository taskApiMappingRepository;
    private final TaskExecutionService taskExecutionService;

    @Transactional
    public String startProcess(String processKey, Map<String, Object> variables) {
        // Get BPMN process
        BpmnProcess bpmnProcess = bpmnDeploymentService.getBpmnProcessByKey(processKey);
        
        // Start process instance
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                processKey,
                variables
        );

        log.info("Started process instance: {} for process: {}", 
                processInstance.getId(), processKey);

        // Execute first task if available
        executeNextTasks(processInstance.getId(), bpmnProcess.getId());

        return processInstance.getId();
    }

    @Transactional
    public void executeTask(String taskId) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }

        String processInstanceId = task.getProcessInstanceId();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance == null) {
            throw new RuntimeException("Process instance not found: " + processInstanceId);
        }

        // Get process variables
        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);

        // Get process definition key using RepositoryService
        String processDefinitionId = processInstance.getProcessDefinitionId();
        String processDefinitionKey = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult()
                .getKey();

        BpmnProcess bpmnProcess = bpmnDeploymentService.getBpmnProcessByKey(processDefinitionKey);

        // Execute task
        TaskApiMapping taskMapping = taskApiMappingRepository
                .findByBpmnProcessIdAndTaskId(bpmnProcess.getId(), task.getTaskDefinitionKey())
                .orElseThrow(() -> new RuntimeException("Task mapping not found for task: " + task.getTaskDefinitionKey()));

        // Execute API call
        Map<String, Object> resultVariables = new HashMap<>(variables);
        taskExecutionService.executeTask(bpmnProcess.getId(), task.getTaskDefinitionKey(), resultVariables);

        // Complete the task with updated variables
        taskService.complete(taskId, resultVariables);

        // Execute next tasks
        executeNextTasks(processInstanceId, bpmnProcess.getId());
    }

    private void executeNextTasks(String processInstanceId, Long bpmnProcessId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();

        for (Task task : tasks) {
            try {
                Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
                TaskApiMapping taskMapping = taskApiMappingRepository
                        .findByBpmnProcessIdAndTaskId(bpmnProcessId, task.getTaskDefinitionKey())
                        .orElseThrow(() -> new RuntimeException("Task mapping not found for task: " + task.getTaskDefinitionKey()));

                // Execute API call
                taskExecutionService.executeTask(bpmnProcessId, task.getTaskDefinitionKey(), variables);

                // Complete the task
                taskService.complete(task.getId(), variables);
            } catch (Exception e) {
                log.error("Error executing task: {}", task.getId(), e);
                // You might want to handle this differently based on your requirements
                break;
            }
        }
    }

    public List<Task> getActiveTasks(String processInstanceId) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .list();
    }
} 