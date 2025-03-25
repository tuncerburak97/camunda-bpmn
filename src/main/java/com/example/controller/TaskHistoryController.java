package com.example.controller;

import com.example.client.CamundaRestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Task geçmişi kontrolcüsü.
 * Aktif olan ve tamamlanmış görevleri izlemek için API endpointlerini sağlar.
 */
@Slf4j
@RestController
@RequestMapping("/api/task-history")
@RequiredArgsConstructor
public class TaskHistoryController {
    
    private final CamundaRestClient camundaRestClient;
    
    /**
     * Aktif olarak çalışan (execute edilen) tüm görevleri döndürür.
     * İsteğe bağlı olarak belirli bir süreç örneği ile filtrelenebilir.
     */
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActiveTasks(
            @RequestParam(required = false) String processInstanceId) {
        try {
            List<Map<String, Object>> activeTasks = camundaRestClient.getActiveTasksHistory(processInstanceId);
            return ResponseEntity.ok(activeTasks);
        } catch (Exception e) {
            log.error("Aktif görevler alınırken hata oluştu: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Daha önce tamamlanmış tüm görevleri döndürür.
     * İsteğe bağlı olarak belirli bir süreç örneği ile filtrelenebilir.
     */
    @GetMapping("/completed")
    public ResponseEntity<List<Map<String, Object>>> getCompletedTasks(
            @RequestParam(required = false) String processInstanceId) {
        try {
            List<Map<String, Object>> completedTasks = camundaRestClient.getCompletedTasksHistory(processInstanceId);
            return ResponseEntity.ok(completedTasks);
        } catch (Exception e) {
            log.error("Tamamlanmış görevler alınırken hata oluştu: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Belirli bir görevin ayrıntılı geçmiş bilgilerini döndürür.
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskHistory(@PathVariable String taskId) {
        try {
            // Önce tamamlanan görevler arasında arayalım
            List<Map<String, Object>> completedTasks = camundaRestClient.getCompletedTasksHistory(null);
            
            for (Map<String, Object> task : completedTasks) {
                if (taskId.equals(task.get("id"))) {
                    return ResponseEntity.ok(task);
                }
            }
            
            // Bulunamadıysa aktif görevler arasında arayalım
            List<Map<String, Object>> activeTasks = camundaRestClient.getActiveTasksHistory(null);
            
            for (Map<String, Object> task : activeTasks) {
                if (taskId.equals(task.get("id"))) {
                    return ResponseEntity.ok(task);
                }
            }
            
            // Eğer görev bulunamadıysa 404 döndürelim
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Görev geçmişi alınırken hata oluştu: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Belirli bir süreç örneğinin hem aktif hem de tamamlanmış tüm görevlerini döndürür.
     */
    @GetMapping("/process/{processInstanceId}")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getAllTasksForProcess(
            @PathVariable String processInstanceId) {
        try {
            List<Map<String, Object>> activeTasks = camundaRestClient.getActiveTasksHistory(processInstanceId);
            List<Map<String, Object>> completedTasks = camundaRestClient.getCompletedTasksHistory(processInstanceId);
            
            Map<String, List<Map<String, Object>>> result = Map.of(
                    "activeTasks", activeTasks,
                    "completedTasks", completedTasks
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Süreç için görevler alınırken hata oluştu: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Tamamlanmış external task'ları getirir.
     * İsteğe bağlı olarak belirli bir süreç örneği ile filtrelenebilir.
     */
    @GetMapping("/external-tasks")
    public ResponseEntity<List<Map<String, Object>>> getExternalTaskHistory(
            @RequestParam(required = false) String processInstanceId) {
        try {
            List<Map<String, Object>> externalTasks = camundaRestClient.getExternalTaskHistory(processInstanceId);
            return ResponseEntity.ok(externalTasks);
        } catch (Exception e) {
            log.error("External task geçmişi alınırken hata oluştu: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Belirli bir süreç için tüm görev türlerini (user tasks ve external tasks) döndürür.
     */
    @GetMapping("/process/{processInstanceId}/all")
    public ResponseEntity<Map<String, Object>> getAllTaskTypesForProcess(
            @PathVariable String processInstanceId) {
        try {
            // Aktif ve tamamlanan user task'ları al
            List<Map<String, Object>> activeTasks = camundaRestClient.getActiveTasksHistory(processInstanceId);
            List<Map<String, Object>> completedTasks = camundaRestClient.getCompletedTasksHistory(processInstanceId);
            
            // External task'ları al
            List<Map<String, Object>> externalTasks = camundaRestClient.getExternalTaskHistory(processInstanceId);
            
            // Aktif external task'ları al
            List<Map<String, Object>> activeExternalTasks = camundaRestClient.getExternalTasksByProcessInstanceId(processInstanceId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("activeUserTasks", activeTasks);
            result.put("completedUserTasks", completedTasks);
            result.put("completedExternalTasks", externalTasks);
            result.put("activeExternalTasks", activeExternalTasks);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Süreç için tüm görev türleri alınırken hata oluştu: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
} 