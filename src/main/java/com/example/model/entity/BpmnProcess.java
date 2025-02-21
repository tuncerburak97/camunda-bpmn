package com.example.model.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "bpmn_processes")
public class BpmnProcess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String processName;

    @Column(nullable = false, unique = true)
    private String processKey;

    @Column(nullable = false)
    private String bpmnFilePath;

    //@Column(nullable = false)
    //private String bpmnDeploymentId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastDeployedAt;

    @Column
    private String description;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 