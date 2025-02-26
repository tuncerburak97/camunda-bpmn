package com.example.service;

import com.example.client.CamundaRestClient;
import com.example.handler.TaskExecutionService;
import com.example.model.entity.BpmnProcess;
import com.example.model.entity.TaskApiMapping;
import com.example.repository.TaskApiMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessExecutionService {
    private final CamundaRestClient camundaRestClient;
    private final BpmnDeploymentService bpmnDeploymentService;
    private final TaskApiMappingRepository taskApiMappingRepository;
    private final TaskExecutionService taskExecutionService;

    @Transactional
    public String startProcess(String processKey, Map<String, Object> variables) {
        // Get BPMN process
        BpmnProcess bpmnProcess = bpmnDeploymentService.getBpmnProcessByKey(processKey);
        
        // Start process instance using REST client
        String processInstanceId = camundaRestClient.startProcess(bpmnProcess.getProcessKey(), variables);
        log.info("Started process instance: {} for process: {}", processInstanceId, processKey);


        try {
            // Küçük bir bekleme ekleyelim
            Thread.sleep(1000);
            
            // Process variables'ı kontrol edelim
            Map<String, Object> processVariables = camundaRestClient.getProcessVariables(processInstanceId);
            log.info("Process variables after start: {}", processVariables);
            
            // External Task'ları kontrol edelim
            List<Map<String, Object>> externalTasks = camundaRestClient.getExternalTasksByProcessInstanceId(processInstanceId);
            log.info("Found {} external tasks for process instance: {}", externalTasks.size(), processInstanceId);
            
            if (externalTasks.isEmpty()) {
                log.warn("No external tasks found for process instance: {}. This might be because:", processInstanceId);
                log.warn("1. The process might not be using External Tasks");
                log.warn("2. The process might have completed immediately");
                log.warn("3. There might be an issue with the process definition");
                log.warn("4. The External Task Worker might not be properly configured");
            }

            return processInstanceId;
        } catch (InterruptedException e) {
            log.error("Error while waiting for tasks", e);
            return processInstanceId;
        }

    }

    @Transactional
    public void executeTask(String taskId) {
        // Get task details
        Map<String, Object> taskDetails = camundaRestClient.getTask(taskId);
        String processInstanceId = (String) taskDetails.get("processInstanceId");
        String taskDefinitionKey = (String) taskDetails.get("taskDefinitionKey");

        // Get process variables
        Map<String, Object> variables = camundaRestClient.getProcessVariables(processInstanceId);

        // Get BPMN process
        BpmnProcess bpmnProcess = bpmnDeploymentService.getBpmnProcessByKey((String) taskDetails.get("processDefinitionKey"));

        // Execute task mapping
        TaskApiMapping taskMapping = taskApiMappingRepository
                .findByBpmnProcessIdAndTaskId(bpmnProcess.getId(), taskDefinitionKey)
                .orElseThrow(() -> new EntityNotFoundException("Task mapping not found for task: " + taskDefinitionKey));

        // Execute API call
        taskExecutionService.executeTask(bpmnProcess.getId(), taskDefinitionKey, variables);

        // Complete the task
        camundaRestClient.completeTask(taskId, variables);
        log.info("Successfully completed task: {}", taskId);

        // Execute next tasks
        executeNextTasks(processInstanceId, bpmnProcess.getId());
    }

    private void executeNextTasks(String processInstanceId, Long bpmnProcessId) {
        List<Map<String, Object>> tasks = camundaRestClient.getTasksByProcessInstanceId(processInstanceId);

        log.info("Found {} tasks to execute for process instance: {}", tasks.size(), processInstanceId);

        for (Map<String, Object> task : tasks) {
            try {
                String taskId = (String) task.get("id");
                String taskName = (String) task.get("name");
                String taskDefinitionKey = (String) task.get("taskDefinitionKey");

                log.info("Executing task: {} ({})", taskName, taskId);

                Map<String, Object> variables = camundaRestClient.getProcessVariables(processInstanceId);
                TaskApiMapping taskMapping = taskApiMappingRepository
                        .findByBpmnProcessIdAndTaskId(bpmnProcessId, taskDefinitionKey)
                        .orElseThrow(() -> new EntityNotFoundException("Task mapping not found for task: " + taskDefinitionKey));

                // Execute API call
                taskExecutionService.executeTask(bpmnProcessId, taskDefinitionKey, variables);

                // Complete the task
                camundaRestClient.completeTask(taskId, variables);
                log.info("Successfully completed task: {}", taskId);
            } catch (Exception e) {
                log.error("Error executing task: {}", task.get("id"), e);
                break;
            }
        }
    }

    public List<Map<String, Object>> getActiveTasks(String processInstanceId) {
        List<Map<String, Object>> tasks = camundaRestClient.getTasksByProcessInstanceId(processInstanceId);
        log.info("Found {} active tasks for process instance: {}", tasks.size(), processInstanceId);
        return tasks;
    }
} 