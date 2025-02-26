package com.example.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition to check if external task client is enabled
 */
public class ExternalTaskEnabledCondition implements Condition {
    
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String enabled = context.getEnvironment().getProperty("camunda.external-task.enabled");
        return "true".equalsIgnoreCase(enabled);
    }
} 