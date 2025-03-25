package com.example.controller;

import com.example.client.CamundaRestClient;
import com.example.service.ProcessExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kullanıcı görevlerini (User Task) yönetmek için controller
 * Bu controller, insanlara atanan BPMN iş akışı görevlerini listeler ve tamamlar
 */
@Slf4j
@RestController
@RequestMapping("/api/user-tasks")
@RequiredArgsConstructor
public class UserTaskController {
    
    private final CamundaRestClient camundaRestClient;
    private final ProcessExecutionService processExecutionService;
    
    /**
     * Belirli bir süreç örneği için tüm aktif kullanıcı görevlerini getirir
     */
    @GetMapping("/process/{processInstanceId}")
    public ResponseEntity<List<Map<String, Object>>> getTasksByProcessInstanceId(
            @PathVariable String processInstanceId) {
        try {
            List<Map<String, Object>> tasks = camundaRestClient.getTasksByProcessInstanceId(processInstanceId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Süreç örneği {} için görevler getirilirken hata oluştu: {}", processInstanceId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Belirli bir görev ID'si için ayrıntılı bilgileri getirir
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskDetails(@PathVariable String taskId) {
        try {
            Map<String, Object> taskDetails = camundaRestClient.getTask(taskId);
            return ResponseEntity.ok(taskDetails);
        } catch (Exception e) {
            log.error("Görev {} için ayrıntılar getirilirken hata oluştu: {}", taskId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Yönetici onayı için kullanıcı görevini tamamlar
     */
    @PostMapping("/{taskId}/approve")
    public ResponseEntity<String> approveTask(
            @PathVariable String taskId,
            @RequestParam(required = false) String comment) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", true);
            variables.put("approvalComment", comment != null ? comment : "Onaylandı");
            
            camundaRestClient.completeTask(taskId, variables);
            
            return ResponseEntity.ok("Görev başarıyla onaylandı");
        } catch (Exception e) {
            log.error("Görev {} onaylanırken hata oluştu: {}", taskId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Görev onaylanırken hata oluştu: " + e.getMessage());
        }
    }
    
    /**
     * Yönetici reddi için kullanıcı görevini tamamlar
     */
    @PostMapping("/{taskId}/reject")
    public ResponseEntity<String> rejectTask(
            @PathVariable String taskId,
            @RequestParam(required = false) String reason) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("approved", false);
            variables.put("approvalComment", reason != null ? reason : "Reddedildi");
            
            camundaRestClient.completeTask(taskId, variables);
            
            return ResponseEntity.ok("Görev başarıyla reddedildi");
        } catch (Exception e) {
            log.error("Görev {} reddedilirken hata oluştu: {}", taskId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Görev reddedilirken hata oluştu: " + e.getMessage());
        }
    }
    
    /**
     * Kullanıcı görevini genel amaçlı tamamlar ve özel değişkenleri iletir
     */
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<String> completeTask(
            @PathVariable String taskId,
            @RequestBody Map<String, Object> variables) {
        try {
            camundaRestClient.completeTask(taskId, variables);
            
            return ResponseEntity.ok("Görev başarıyla tamamlandı");
        } catch (Exception e) {
            log.error("Görev {} tamamlanırken hata oluştu: {}", taskId, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Görev tamamlanırken hata oluştu: " + e.getMessage());
        }
    }
} 