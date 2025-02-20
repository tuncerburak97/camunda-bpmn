package com.example.repository;

import com.example.model.entity.BpmnProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BpmnProcessRepository extends JpaRepository<BpmnProcess, Long> {
    Optional<BpmnProcess> findByProcessKey(String processKey);
    boolean existsByProcessKey(String processKey);
} 