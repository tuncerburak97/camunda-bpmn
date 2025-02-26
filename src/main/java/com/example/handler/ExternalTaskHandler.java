package com.example.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import com.example.repository.TaskApiMappingRepository;
import com.example.model.entity.TaskApiMapping;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(ExternalTaskClient.class)
public class ExternalTaskHandler {

    private final ExternalTaskClient externalTaskClient;
    private final TaskExecutionService taskExecutionService;
    private final TaskApiMappingRepository taskApiMappingRepository;
    
    @Value("${camunda.external-task.enabled:false}")
    private boolean externalTaskEnabled;
    
    @Value("${camunda.external-task.lock-duration:20000}")
    private long lockDuration;
    
    // Aktif subscription'ları izlemek için map
    private final Map<String, Boolean> activeSubscriptions = new ConcurrentHashMap<>();

    @PostConstruct
    public void subscribeToTopics() {
        if (!externalTaskEnabled) {
            log.info("External task client is disabled. Skipping subscription to topics.");
            return;
        }
        
        log.info("Starting generic external task handler...");
        
        try {
            // Veritabanından tüm task mapping'leri al
            refreshSubscriptions();
            
            log.info("Successfully subscribed to all external tasks");
        } catch (Exception e) {
            log.error("Failed to subscribe to external tasks: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Aktif subscription'ları döndürür
     * @return Aktif subscription'ların listesi
     */
    public Set<String> getActiveSubscriptions() {
        return activeSubscriptions.keySet();
    }
    
    /**
     * Tüm topic subscription'ları yeniler
     * Bu metot periyodik olarak çağrılabilir veya yeni bir BPMN süreci deploy edildiğinde tetiklenebilir
     */
    public void refreshSubscriptions() {
        try {
            // Veritabanından tüm task mapping'leri al
            List<TaskApiMapping> allMappings = taskApiMappingRepository.findAll();
            
            // Tüm task ID'lerini topla (bunlar topic isimleri olarak kullanılacak)
            Set<String> allTaskIds = allMappings.stream()
                    .map(TaskApiMapping::getTaskId)
                    .filter(taskId -> taskId != null && !taskId.isEmpty())
                    .collect(Collectors.toSet());
            
            log.info("Found {} task mappings in database", allTaskIds.size());
            
            // Her bir task ID için subscription oluştur (eğer zaten yoksa)
            for (String taskId : allTaskIds) {
                if (!activeSubscriptions.containsKey(taskId)) {
                    log.info("Creating new subscription for topic: {}", taskId);
                    
                    TopicSubscriptionBuilder subscriptionBuilder = externalTaskClient.subscribe(taskId)
                        .lockDuration(lockDuration)
                        .handler(this::handleExternalTask);
                    
                    subscriptionBuilder.open();
                    activeSubscriptions.put(taskId, true);
                    
                    log.info("Successfully subscribed to topic: {}", taskId);
                }
            }
            
            // Artık kullanılmayan subscription'ları logla (şu an için silmiyoruz)
            Set<String> unusedTopics = new HashSet<>(activeSubscriptions.keySet());
            unusedTopics.removeAll(allTaskIds);
            
            if (!unusedTopics.isEmpty()) {
                log.warn("Found {} unused topic subscriptions that might need cleanup: {}", 
                        unusedTopics.size(), unusedTopics);
            }
            
        } catch (Exception e) {
            log.error("Error refreshing topic subscriptions", e);
        }
    }

    private void handleExternalTask(org.camunda.bpm.client.task.ExternalTask externalTask,
                                  org.camunda.bpm.client.task.ExternalTaskService externalTaskService) {
        String taskId = externalTask.getActivityId();
        String processInstanceId = externalTask.getProcessInstanceId();
        String topicName = externalTask.getTopicName();
        
        log.info("Handling external task: {} (topic: {}) for process instance: {}", 
                taskId, topicName, processInstanceId);
        
        try {
            // Get all variables from the task
            Map<String, Object> variables = externalTask.getAllVariables();
            log.info("Task variables received from process instance: {}", variables);
            
            // Find task mapping
            taskApiMappingRepository.findByTaskId(taskId)
                .ifPresentOrElse(
                    mapping -> {
                        try {
                            // Execute the task using mapping
                            Map<String, Object> result = taskExecutionService.executeTask(
                                mapping.getBpmnProcess() != null ? mapping.getBpmnProcess().getId() : null, 
                                taskId, 
                                variables
                            );
                            
                            // Log the result variables for debugging
                            log.info("Task execution result variables to be sent back to process instance: {}", result);
                            
                            // Complete the task with results
                            // Ensure all variables are passed back to the process instance
                            externalTaskService.complete(externalTask, result);
                            log.info("Successfully completed task: {} (topic: {}) for process instance: {}", 
                                taskId, topicName, processInstanceId);
                        } catch (Exception e) {
                            handleTaskError(externalTask, externalTaskService, e);
                        }
                    },
                    () -> {
                        log.warn("No task mapping found for task: {} (topic: {}) in process: {}", 
                            taskId, topicName, processInstanceId);
                        // Complete the task without executing any API call
                        externalTaskService.complete(externalTask);
                    }
                );
        } catch (Exception e) {
            handleTaskError(externalTask, externalTaskService, e);
        }
    }

    private void handleTaskError(org.camunda.bpm.client.task.ExternalTask externalTask, 
                               org.camunda.bpm.client.task.ExternalTaskService externalTaskService, 
                               Exception e) {
        Map<String, Object> errorVariables = new HashMap<>();
        errorVariables.put("error", e.getMessage());
        errorVariables.put("errorTimestamp", System.currentTimeMillis());
        errorVariables.put("errorDetails", e.toString());
        
        externalTaskService.handleFailure(externalTask,
                e.getMessage(),
                e.getStackTrace().toString(),
                3, // retries
                60000L); // retry timeout in milliseconds
        
        log.error("Error handling task: {} (topic: {}) for process instance: {}", 
            externalTask.getActivityId(), 
            externalTask.getTopicName(),
            externalTask.getProcessInstanceId(), 
            e);
    }
} 