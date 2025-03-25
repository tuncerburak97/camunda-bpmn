package com.example.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * BPMN süreç bilgilerini taşıyan DTO sınıfı
 * Bu sınıf, UI katmanına veya diğer servislere veri transferi için kullanılır
 */
@Data
public class BpmnProcessDTO {
    private Long id;
    private String processKey;
    private String processName;
    private String description;
    private String deploymentId;
    private LocalDateTime lastDeployedAt;
    private Boolean enabled;
} 