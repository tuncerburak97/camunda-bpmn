package com.example.controller;

import com.example.service.GatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing Camunda gateways
 */
@Slf4j
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class GatewayController {
    
    private final GatewayService gatewayService;
    
    /**
     * Get all gateways in a process instance
     */
    @GetMapping("/process-instance/{processInstanceId}")
    public ResponseEntity<List<Map<String, Object>>> getGateways(@PathVariable String processInstanceId) {
        try {
            List<Map<String, Object>> gateways = gatewayService.getGateways(processInstanceId);
            return ResponseEntity.ok(gateways);
        } catch (Exception e) {
            log.error("Error getting gateways for process instance: {}", processInstanceId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Execute a gateway with specified target activities
     */
    @PostMapping("/process-instance/{processInstanceId}/execute")
    public ResponseEntity<Void> executeGateway(
            @PathVariable String processInstanceId,
            @RequestParam String gatewayId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<String> targetActivityIds = (List<String>) requestBody.get("targetActivityIds");
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) requestBody.get("variables");
            
            gatewayService.executeGateway(processInstanceId, gatewayId, targetActivityIds, variables);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error executing gateway for process instance: {}", processInstanceId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Execute an exclusive gateway
     */
    @PostMapping("/process-instance/{processInstanceId}/exclusive")
    public ResponseEntity<Void> executeExclusiveGateway(
            @PathVariable String processInstanceId,
            @RequestParam String gatewayId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            String targetActivityId = (String) requestBody.get("targetActivityId");
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) requestBody.get("variables");
            
            gatewayService.executeExclusiveGateway(processInstanceId, gatewayId, targetActivityId, variables);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error executing exclusive gateway for process instance: {}", processInstanceId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Execute a parallel gateway
     */
    @PostMapping("/process-instance/{processInstanceId}/parallel")
    public ResponseEntity<Void> executeParallelGateway(
            @PathVariable String processInstanceId,
            @RequestParam String gatewayId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<String> targetActivityIds = (List<String>) requestBody.get("targetActivityIds");
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) requestBody.get("variables");
            
            gatewayService.executeParallelGateway(processInstanceId, gatewayId, targetActivityIds, variables);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error executing parallel gateway for process instance: {}", processInstanceId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Execute an inclusive gateway
     */
    @PostMapping("/process-instance/{processInstanceId}/inclusive")
    public ResponseEntity<Void> executeInclusiveGateway(
            @PathVariable String processInstanceId,
            @RequestParam String gatewayId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<String> targetActivityIds = (List<String>) requestBody.get("targetActivityIds");
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) requestBody.get("variables");
            
            gatewayService.executeInclusiveGateway(processInstanceId, gatewayId, targetActivityIds, variables);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error executing inclusive gateway for process instance: {}", processInstanceId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Execute an event-based gateway
     */
    @PostMapping("/process-instance/{processInstanceId}/event-based")
    public ResponseEntity<Void> executeEventBasedGateway(
            @PathVariable String processInstanceId,
            @RequestParam String gatewayId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            String targetActivityId = (String) requestBody.get("targetActivityId");
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) requestBody.get("variables");
            
            gatewayService.executeEventBasedGateway(processInstanceId, gatewayId, targetActivityId, variables);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error executing event-based gateway for process instance: {}", processInstanceId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Execute a complex gateway
     */
    @PostMapping("/process-instance/{processInstanceId}/complex")
    public ResponseEntity<Void> executeComplexGateway(
            @PathVariable String processInstanceId,
            @RequestParam String gatewayId,
            @RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<String> targetActivityIds = (List<String>) requestBody.get("targetActivityIds");
            @SuppressWarnings("unchecked")
            Map<String, Object> variables = (Map<String, Object>) requestBody.get("variables");
            
            gatewayService.executeComplexGateway(processInstanceId, gatewayId, targetActivityIds, variables);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error executing complex gateway for process instance: {}", processInstanceId, e);
            return ResponseEntity.badRequest().build();
        }
    }
} 