package com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalTaskHandler {

    private final ExternalTaskClient externalTaskClient;
    private final TaskExecutionService taskExecutionService;

    @PostConstruct
    public void subscribeToTopics() {
        // Login task handler
        subscribeToTopic("login", (externalTask, externalTaskService) -> {
            try {
                log.info("Executing login task for process instance: {}", 
                        externalTask.getProcessInstanceId());

                // Get variables
                Map<String, Object> variables = new HashMap<>();
                String userId = externalTask.getVariable("username");
                String password = externalTask.getVariable("password");
                
                variables.put("userId", userId);
                variables.put("password", password);

                // Execute business logic
                Map<String, Object> result = taskExecutionService.executeLogin(variables);
                
                // Set result variables
                variables.putAll(result);

                // Complete the task with variables
                externalTaskService.complete(externalTask, variables);
                log.info("Login task completed successfully for process instance: {}", 
                        externalTask.getProcessInstanceId());
            } catch (Exception e) {
                log.error("Error executing login task", e);
                handleTaskError(externalTask, externalTaskService, e);
            }
        });

        // Get profile task handler
        subscribeToTopic("get-profile", (externalTask, externalTaskService) -> {
            try {
                log.info("Executing get-profile task for process instance: {}", 
                        externalTask.getProcessInstanceId());

                // Get variables
                Map<String, Object> variables = new HashMap<>();
                String userId = externalTask.getVariable("userId");
                String token = externalTask.getVariable("token");
                
                variables.put("userId", userId);
                variables.put("token", token);

                // Execute business logic
                Map<String, Object> result = taskExecutionService.executeGetProfile(variables);
                
                // Set result variables
                variables.putAll(result);

                // Complete the task with variables
                externalTaskService.complete(externalTask, variables);
                log.info("Get profile task completed successfully for process instance: {}", 
                        externalTask.getProcessInstanceId());
            } catch (Exception e) {
                log.error("Error executing get-profile task", e);
                handleTaskError(externalTask, externalTaskService, e);
            }
        });
    }

    private void subscribeToTopic(String topicName, ExternalTaskHandler.TaskHandler handler) {
        TopicSubscriptionBuilder builder = externalTaskClient.subscribe(topicName);
        builder.handler((externalTask, externalTaskService) -> {
            try {
                handler.handle(externalTask, externalTaskService);
            } catch (Exception e) {
                log.error("Error in task handler for topic: {}", topicName, e);
                handleTaskError(externalTask, externalTaskService, e);
            }
        });
        builder.open();
        log.info("Subscribed to topic: {}", topicName);
    }

    private void handleTaskError(org.camunda.bpm.client.task.ExternalTask externalTask, 
                               org.camunda.bpm.client.task.ExternalTaskService externalTaskService, 
                               Exception e) {
        Map<String, Object> errorVariables = new HashMap<>();
        errorVariables.put("error", e.getMessage());
        errorVariables.put("errorTimestamp", System.currentTimeMillis());
        
        externalTaskService.handleFailure(externalTask,
                e.getMessage(),
                e.getStackTrace().toString(),
                3, // retries
                60000L); // retry timeout in milliseconds
    }

    @FunctionalInterface
    private interface TaskHandler {
        void handle(org.camunda.bpm.client.task.ExternalTask externalTask,
                   org.camunda.bpm.client.task.ExternalTaskService externalTaskService) throws Exception;
    }
} 