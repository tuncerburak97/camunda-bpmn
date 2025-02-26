package com.example.config;

import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.PropertySource;
import java.util.Collections;

@Configuration
@PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true)
public class ExternalTaskConfig {

    @Value("${camunda.rest.url:http://localhost:8080/engine-rest}")
    private String camundaRestUrl;
    
    @Value("${camunda.external-task.enabled:false}")
    private boolean externalTaskEnabled;
    
    @Value("${camunda.external-task.lock-duration:20000}")
    private long lockDuration;
    
    @Value("${camunda.external-task.async-response-timeout:10000}")
    private long asyncResponseTimeout;
    
    @Value("${camunda.external-task.max-tasks:1}")
    private int maxTasks;
    
    @Value("${camunda.external-task.worker-id:generic-external-task-worker}")
    private String workerId;

    @Bean
    @Conditional(ExternalTaskEnabledCondition.class)
    public ExternalTaskClient externalTaskClient() {
        return ExternalTaskClient.create()
                .baseUrl(camundaRestUrl)
                .asyncResponseTimeout(asyncResponseTimeout)
                .maxTasks(maxTasks)
                .defaultSerializationFormat("application/json")
                .lockDuration(lockDuration)
                .workerId(workerId)
                .build();
    }
} 