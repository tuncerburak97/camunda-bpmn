package com.example.service;

import com.example.model.entity.BpmnProcess;
import com.example.repository.BpmnProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BpmnDeploymentService {
    private final BpmnProcessRepository bpmnProcessRepository;
    private static final String BPMN_STORAGE_PATH = "bpmn-files/";

    @Transactional
    public BpmnProcess deployBpmnProcess(String processName, String processKey, MultipartFile bpmnFile, String description) throws IOException {
        // Create storage directory if it doesn't exist
        Path storagePath = Paths.get(BPMN_STORAGE_PATH);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }

        // Save BPMN file
        String fileName = processKey + "_" + System.currentTimeMillis() + ".bpmn";
        Path filePath = storagePath.resolve(fileName);
        Files.copy(bpmnFile.getInputStream(), filePath);

        // Create or update BPMN process
        BpmnProcess bpmnProcess = bpmnProcessRepository.findByProcessKey(processKey)
                .orElse(new BpmnProcess());

        bpmnProcess.setProcessName(processName);
        bpmnProcess.setProcessKey(processKey);
        bpmnProcess.setBpmnFilePath(filePath.toString());
        bpmnProcess.setDescription(description);
        bpmnProcess.setLastDeployedAt(LocalDateTime.now());

        return bpmnProcessRepository.save(bpmnProcess);
    }

    public File getBpmnFile(String processKey) {
        BpmnProcess bpmnProcess = bpmnProcessRepository.findByProcessKey(processKey)
                .orElseThrow(() -> new RuntimeException("BPMN process not found: " + processKey));

        File bpmnFile = new File(bpmnProcess.getBpmnFilePath());
        if (!bpmnFile.exists()) {
            throw new RuntimeException("BPMN file not found: " + bpmnProcess.getBpmnFilePath());
        }

        return bpmnFile;
    }

    public List<BpmnProcess> getAllBpmnProcesses() {
        return bpmnProcessRepository.findAll();
    }

    public BpmnProcess getBpmnProcessByKey(String processKey) {
        return bpmnProcessRepository.findByProcessKey(processKey)
                .orElseThrow(() -> new RuntimeException("BPMN process not found: " + processKey));
    }
} 