package com.example.service;

import com.example.client.CamundaRestClient;
import com.example.handler.ExternalTaskHandler;
import com.example.model.client.DeploymentResponse;
import com.example.model.entity.BpmnProcess;
import com.example.repository.BpmnProcessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import org.springframework.context.ApplicationContext;

@Slf4j
@Service
@RequiredArgsConstructor
public class BpmnDeploymentService {
    private final BpmnProcessRepository bpmnProcessRepository;
    private final CamundaRestClient camundaRestClient;
    private static final String BPMN_STORAGE_PATH = "bpmn-files/";
    private final ApplicationContext applicationContext;

    @Transactional
    public BpmnProcess deployBpmnProcess(String processName, String processKey, MultipartFile bpmnFile, String description) throws IOException {
        // Create storage directory if it doesn't exist
        Path storagePath = Paths.get(BPMN_STORAGE_PATH);
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }

        // Save BPMN file
        String fileName = processKey + ".bpmn";
        Path filePath = storagePath.resolve(fileName);
        Files.copy(bpmnFile.getInputStream(), filePath);

        // Deploy to Camunda Engine using REST client
        try {
            camundaRestClient.deployProcess(processName, filePath.toFile());
            log.info("Successfully deployed BPMN process to Camunda Engine: {}", processKey);
            // Create or update BPMN process
            BpmnProcess bpmnProcess = bpmnProcessRepository.findByProcessKey(processKey)
                    .orElse(new BpmnProcess());

            bpmnProcess.setProcessName(processName);
            bpmnProcess.setProcessKey(processKey);
            bpmnProcess.setBpmnFilePath(filePath.toString());
            bpmnProcess.setDescription(description);
            bpmnProcess.setLastDeployedAt(LocalDateTime.now());
            //bpmnProcess.setBpmnDeploymentId(deploy.getId());

            // Yeni süreç deploy edildiğinde external task subscription'ları yenile
            refreshExternalTaskSubscriptions();

            return bpmnProcessRepository.save(bpmnProcess);

        } catch (Exception e) {
            log.error("Failed to deploy BPMN process to Camunda Engine: {}", processKey, e);
            throw new RuntimeException("Failed to deploy to Camunda Engine", e);
        }
    }
    
    @Transactional
    public BpmnProcess deployBpmnProcess(String processName, String processKey, String bpmnXml) {
        // Create storage directory if it doesn't exist
        try {
            Path storagePath = Paths.get(BPMN_STORAGE_PATH);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            
            // Save BPMN file
            String fileName = processKey + ".bpmn";
            Path filePath = storagePath.resolve(fileName);
            Files.write(filePath, bpmnXml.getBytes(StandardCharsets.UTF_8));
            
            // Deploy to Camunda Engine using REST client
            File bpmnFile = filePath.toFile();
            camundaRestClient.deployProcess(processName, bpmnFile);
            log.info("Successfully deployed BPMN process to Camunda Engine: {}", processKey);
            
            // Create or update BPMN process
            BpmnProcess bpmnProcess = bpmnProcessRepository.findByProcessKey(processKey)
                    .orElse(new BpmnProcess());
            
            bpmnProcess.setProcessName(processName);
            bpmnProcess.setProcessKey(processKey);
            bpmnProcess.setBpmnFilePath(filePath.toString());
            bpmnProcess.setDescription("Deployed from XML string");
            bpmnProcess.setLastDeployedAt(LocalDateTime.now());
            
            // Yeni süreç deploy edildiğinde external task subscription'ları yenile
            refreshExternalTaskSubscriptions();
            
            return bpmnProcessRepository.save(bpmnProcess);
            
        } catch (Exception e) {
            log.error("Failed to deploy BPMN process to Camunda Engine: {}", processKey, e);
            throw new RuntimeException("Failed to deploy to Camunda Engine", e);
        }
    }

    /**
     * External task subscription'larını yeniler
     * Bu metot, ExternalTaskHandler'ı çağırarak tüm subscription'ları günceller
     */
    private void refreshExternalTaskSubscriptions() {
        try {
            // ExternalTaskHandler bean'ini al
            ExternalTaskHandler externalTaskHandler = applicationContext.getBean(ExternalTaskHandler.class);
            
            // Subscription'ları yenile
            externalTaskHandler.refreshSubscriptions();
            
            log.info("External task subscriptions refreshed after BPMN deployment");
        } catch (Exception e) {
            // ExternalTaskHandler bean'i bulunamadıysa veya devre dışı bırakıldıysa hata verme
            log.warn("Could not refresh external task subscriptions: {}", e.getMessage());
        }
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

    public List<DeploymentResponse> getDeployments(
            String id,
            String name,
            String nameLike,
            String source,
            Boolean withoutSource,
            String tenantIdIn,
            Boolean withoutTenantId,
            Boolean includeDeploymentsWithoutTenantId,
            String after,
            String before,
            String sortBy,
            String sortOrder,
            Integer firstResult,
            Integer maxResults
    ) {
        return camundaRestClient.getDeployments(id, name, nameLike, source, withoutSource, tenantIdIn,
                withoutTenantId, includeDeploymentsWithoutTenantId, after, before, sortBy, sortOrder,
                firstResult, maxResults);
    }

    public List<String> deleteDeployment(List<String> deploymentIDs){
        List<String> response = new ArrayList<>();
        for (var deployId : deploymentIDs){
            try {
                camundaRestClient.deleteDeployment(deployId,true);
                response.add(deployId);
            }catch (Exception e){
                log.error("Error delete deployment {}",deployId,e);
            }
        }
        return response;
    }
}