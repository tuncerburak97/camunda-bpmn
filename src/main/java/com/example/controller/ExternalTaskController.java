package com.example.controller;

import com.example.handler.ExternalTaskHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/external-task")
@RequiredArgsConstructor
public class ExternalTaskController {

    private final ExternalTaskHandler externalTaskHandler;
    
    @Value("${camunda.external-task.enabled:false}")
    private boolean externalTaskEnabled;

    /**
     * Refreshes external task subscriptions
     * This endpoint can be used when a new BPMN process is deployed or
     * when you want to update existing subscriptions
     */
    @PostMapping("/refresh-subscriptions")
    public ResponseEntity<Map<String, Object>> refreshSubscriptions() {
        Map<String, Object> response = new HashMap<>();
        
        if (!externalTaskEnabled) {
            response.put("success", false);
            response.put("message", "External task client is disabled. Enable it in application.yml.");
            return ResponseEntity.ok(response);
        }
        
        try {
            externalTaskHandler.refreshSubscriptions();
            response.put("success", true);
            response.put("message", "Successfully refreshed external task subscriptions");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error refreshing external task subscriptions", e);
            response.put("success", false);
            response.put("message", "Error refreshing subscriptions: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Returns the status of the external task client
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", externalTaskEnabled);
        
        if (externalTaskEnabled) {
            status.put("activeSubscriptions", externalTaskHandler.getActiveSubscriptions());
        }
        
        return ResponseEntity.ok(status);
    }
} 