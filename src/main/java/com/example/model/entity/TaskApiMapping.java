package com.example.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "task_api_mappings")
public class TaskApiMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bpmn_process_id", nullable = false)
    private BpmnProcess bpmnProcess;

    @Column(nullable = false)
    private String taskId;

    @Column(nullable = false)
    private String taskName;

    @Column(nullable = false)
    private String apiUrl;

    @Column(nullable = false)
    private String httpMethod;

    @Column(columnDefinition = "TEXT")
    private String requestTemplate;

    @Column(columnDefinition = "TEXT")
    private String responseMapping;

    @Column(columnDefinition = "TEXT")
    private String headers;

    // Retry configuration
    @Column(nullable = false)
    private Integer maxRetries = 3;

    @Column(nullable = false)
    private Long retryTimeout = 60000L; // milliseconds

    @Column(nullable = false)
    private Long timeout = 30000L; // API call timeout in milliseconds

    // Error handling
    @Column(nullable = false)
    private Boolean failOnError = true;

    @Column(columnDefinition = "TEXT")
    private String errorMapping;

    // Validation
    @Column(columnDefinition = "TEXT")
    private String requestSchema;

    @Column(columnDefinition = "TEXT")
    private String responseSchema;

    // Metadata
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 