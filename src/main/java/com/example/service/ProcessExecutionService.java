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
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessExecutionService {
    private final CamundaRestClient camundaRestClient;
    private final BpmnDeploymentService bpmnDeploymentService;
    private final TaskApiMappingRepository taskApiMappingRepository;
    private final TaskExecutionService taskExecutionService;
    private final GatewayService gatewayService;

    @Transactional
    public String startProcess(String processKey, Map<String, Object> variables) {
        // Get BPMN process
        BpmnProcess bpmnProcess = bpmnDeploymentService.getBpmnProcessByKey(processKey);
        
        // Start process instance using REST client
        String processInstanceId = camundaRestClient.startProcess(bpmnProcess.getProcessKey(), variables);
        log.info("Started process instance: {} for process: {}", processInstanceId, processKey);


        try {
            // Add a small delay
            Thread.sleep(100);
            
            // Check process variable
            /*
            Map<String, Object> processVariables = camundaRestClient.getProcessVariables(processInstanceId);
            log.info("Process variables after start: {}", processVariables);
            
            // Check external tasks
            List<Map<String, Object>> externalTasks = camundaRestClient.getExternalTasksByProcessInstanceId(processInstanceId);
            log.info("Found {} external tasks for process instance: {}", externalTasks.size(), processInstanceId);
            
            if (externalTasks.isEmpty()) {
                log.warn("No external tasks found for process instance: {}. This might be because:", processInstanceId);
                log.warn("1. The process might not be using External Tasks");
                log.warn("2. The process might have completed immediately");
                log.warn("3. There might be an issue with the process definition");
                log.warn("4. The External Task Worker might not be properly configured");
            }
            
            // Check for active gateways
            //checkAndHandleActiveGateways(processInstanceId);


             */
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
        
        // Check for active gateways
        //checkAndHandleActiveGateways(processInstanceId);
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
    
    /**
     * Check for active gateways in the process instance and handle them if needed
     */
    public void checkAndHandleActiveGateways(String processInstanceId) {
        try {
            // Get all gateways in the process instance
            List<Map<String, Object>> gateways = gatewayService.getGateways(processInstanceId);
            
            // Filter active gateways
            List<Map<String, Object>> activeGateways = new ArrayList<>();
            for (Map<String, Object> gateway : gateways) {
                if (Boolean.TRUE.equals(gateway.get("active"))) {
                    activeGateways.add(gateway);
                }
            }
            
            log.info("Found {} active gateways in process instance: {}", activeGateways.size(), processInstanceId);
            
            // Handle each active gateway
            for (Map<String, Object> gateway : activeGateways) {
                handleGateway(processInstanceId, gateway);
            }
        } catch (Exception e) {
            log.error("Error checking for active gateways in process instance: {}", processInstanceId, e);
        }
    }
    
    /**
     * Handle a specific gateway based on its type
     */
    private void handleGateway(String processInstanceId, Map<String, Object> gateway) {
        String gatewayId = (String) gateway.get("id");
        String gatewayType = (String) gateway.get("type");
        
        log.info("Handling gateway of type {} with ID {} in process instance: {}", 
                gatewayType, gatewayId, processInstanceId);
        
        try {
            // Get process variables
            Map<String, Object> variables = camundaRestClient.getProcessVariables(processInstanceId);
            
            // Handle gateway based on type
            switch (gatewayType) {
                case "exclusiveGateway":
                    handleExclusiveGateway(processInstanceId, gateway, variables);
                    break;
                case "parallelGateway":
                    handleParallelGateway(processInstanceId, gateway, variables);
                    break;
                case "inclusiveGateway":
                    handleInclusiveGateway(processInstanceId, gateway, variables);
                    break;
                case "eventBasedGateway":
                    // Event-based gateways are typically handled by events, not direct execution
                    log.info("Event-based gateway {} requires external events to proceed", gatewayId);
                    break;
                case "complexGateway":
                    // Complex gateways require custom logic
                    log.info("Complex gateway {} requires custom handling logic", gatewayId);
                    break;
                default:
                    log.warn("Unknown gateway type: {} for gateway: {}", gatewayType, gatewayId);
            }
        } catch (Exception e) {
            log.error("Error handling gateway {} in process instance: {}", gatewayId, processInstanceId, e);
        }
    }
    
    /**
     * Handle an exclusive gateway
     * For automatic handling, we need to determine which path to take based on variables
     * This is a simplified implementation that would need to be customized for real use cases
     */
    private void handleExclusiveGateway(String processInstanceId, Map<String, Object> gateway, Map<String, Object> variables) {
        String gatewayId = (String) gateway.get("id");
        
        // In a real implementation, you would:
        // 1. Parse the BPMN XML to find the conditions on each outgoing sequence flow
        // 2. Evaluate each condition against the current variables
        // 3. Select the path with the first condition that evaluates to true
        
        log.info("Exclusive gateway {} requires manual decision or condition evaluation", gatewayId);
        
        // For now, we'll just log that manual intervention is needed
        // In a real implementation, you might want to:
        // - Look up a configuration for this gateway
        // - Evaluate JavaScript conditions from the BPMN
        // - Use a rules engine
        // - Call an external service to make the decision
    }
    
    /**
     * Handle a parallel gateway
     * Parallel gateways should activate all outgoing paths
     */
    private void handleParallelGateway(String processInstanceId, Map<String, Object> gateway, Map<String, Object> variables) {
        String gatewayId = (String) gateway.get("id");
        
        // For parallel gateways, we need to find all outgoing paths and activate them
        // This would require parsing the BPMN XML to find the target activities
        
        log.info("Parallel gateway {} should activate all outgoing paths", gatewayId);
        
        // In a real implementation, you would:
        // 1. Parse the BPMN XML to find all outgoing sequence flows
        // 2. Determine the target activities for each flow
        // 3. Activate all target activities
    }
    
    /**
     * Handle an inclusive gateway
     * For automatic handling, we need to determine which paths to take based on variables
     */
    private void handleInclusiveGateway(String processInstanceId, Map<String, Object> gateway, Map<String, Object> variables) {
        String gatewayId = (String) gateway.get("id");
        
        // Similar to exclusive gateway, but multiple paths can be taken
        
        log.info("Inclusive gateway {} requires condition evaluation for multiple paths", gatewayId);
        
        // In a real implementation, you would:
        // 1. Parse the BPMN XML to find the conditions on each outgoing sequence flow
        // 2. Evaluate each condition against the current variables
        // 3. Select all paths with conditions that evaluate to true
    }
} 