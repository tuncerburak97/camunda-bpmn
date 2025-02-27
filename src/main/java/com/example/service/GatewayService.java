package com.example.service;

import com.example.client.CamundaRestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;

/**
 * Service for handling Camunda gateways
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GatewayService {
    
    private final CamundaRestClient camundaRestClient;
    
    // Gateway types in BPMN 2.0
    private static final String EXCLUSIVE_GATEWAY = "exclusiveGateway";
    private static final String PARALLEL_GATEWAY = "parallelGateway";
    private static final String INCLUSIVE_GATEWAY = "inclusiveGateway";
    private static final String EVENT_BASED_GATEWAY = "eventBasedGateway";
    private static final String COMPLEX_GATEWAY = "complexGateway";
    
    /**
     * Get all gateways in a process instance
     */
    public List<Map<String, Object>> getGateways(String processInstanceId) {
        try {
            // Get process definition ID
            Map<String, Object> activityInstances = camundaRestClient.getActivityInstances(processInstanceId);
            String processDefinitionId = (String) activityInstances.get("processDefinitionId");
            
            // Get BPMN XML
            String bpmnXml = camundaRestClient.getProcessDefinitionXml(processDefinitionId);
            
            // Parse XML to find gateways
            List<Map<String, Object>> gateways = parseGatewaysFromXml(bpmnXml);
            
            // Enrich with activity instance information
            enrichGatewaysWithInstanceInfo(gateways, activityInstances);
            
            return gateways;
        } catch (Exception e) {
            log.error("Error getting gateways for process instance: {}", processInstanceId, e);
            throw new RuntimeException("Failed to get gateways", e);
        }
    }
    
    /**
     * Execute a specific gateway by activating one or more outgoing paths
     */
    public void executeGateway(String processInstanceId, String gatewayId, List<String> targetActivityIds, Map<String, Object> variables) {
        try {
            List<Map<String, Object>> instructions = new ArrayList<>();
            
            // For each target activity, create an instruction to start before that activity
            for (String activityId : targetActivityIds) {
                Map<String, Object> instruction = new HashMap<>();
                instruction.put("type", "startBeforeActivity");
                instruction.put("activityId", activityId);
                
                // Add variables if provided
                if (variables != null && !variables.isEmpty()) {
                    instruction.put("variables", formatVariablesForModification(variables));
                }
                
                instructions.add(instruction);
            }
            
            // Cancel the current gateway activity instance
            Map<String, Object> activityInstances = camundaRestClient.getActivityInstances(processInstanceId);
            addCancelInstructionsForGateway(instructions, activityInstances, gatewayId);
            
            // Execute the modification
            camundaRestClient.modifyProcessInstance(processInstanceId, instructions);
            
            log.info("Successfully executed gateway {} in process instance {}", gatewayId, processInstanceId);
        } catch (Exception e) {
            log.error("Error executing gateway {} in process instance {}", gatewayId, processInstanceId, e);
            throw new RuntimeException("Failed to execute gateway", e);
        }
    }
    
    /**
     * Execute an exclusive gateway by selecting one outgoing path
     */
    public void executeExclusiveGateway(String processInstanceId, String gatewayId, String targetActivityId, Map<String, Object> variables) {
        executeGateway(processInstanceId, gatewayId, Collections.singletonList(targetActivityId), variables);
    }
    
    /**
     * Execute a parallel gateway by activating all outgoing paths
     */
    public void executeParallelGateway(String processInstanceId, String gatewayId, List<String> targetActivityIds, Map<String, Object> variables) {
        executeGateway(processInstanceId, gatewayId, targetActivityIds, variables);
    }
    
    /**
     * Execute an inclusive gateway by selecting one or more outgoing paths
     */
    public void executeInclusiveGateway(String processInstanceId, String gatewayId, List<String> targetActivityIds, Map<String, Object> variables) {
        executeGateway(processInstanceId, gatewayId, targetActivityIds, variables);
    }
    
    /**
     * Execute an event-based gateway by selecting one outgoing path
     */
    public void executeEventBasedGateway(String processInstanceId, String gatewayId, String targetActivityId, Map<String, Object> variables) {
        executeGateway(processInstanceId, gatewayId, Collections.singletonList(targetActivityId), variables);
    }
    
    /**
     * Execute a complex gateway with custom logic
     */
    public void executeComplexGateway(String processInstanceId, String gatewayId, List<String> targetActivityIds, Map<String, Object> variables) {
        executeGateway(processInstanceId, gatewayId, targetActivityIds, variables);
    }
    
    /**
     * Parse BPMN XML to find gateways
     */
    private List<Map<String, Object>> parseGatewaysFromXml(String bpmnXml) {
        List<Map<String, Object>> gateways = new ArrayList<>();
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(bpmnXml)));
            
            // Find all gateway types
            findGatewaysByType(document, EXCLUSIVE_GATEWAY, "Exclusive Gateway (XOR)", gateways);
            findGatewaysByType(document, PARALLEL_GATEWAY, "Parallel Gateway (AND)", gateways);
            findGatewaysByType(document, INCLUSIVE_GATEWAY, "Inclusive Gateway (OR)", gateways);
            findGatewaysByType(document, EVENT_BASED_GATEWAY, "Event-Based Gateway", gateways);
            findGatewaysByType(document, COMPLEX_GATEWAY, "Complex Gateway", gateways);
            
        } catch (Exception e) {
            log.error("Error parsing BPMN XML", e);
        }
        
        return gateways;
    }
    
    /**
     * Find gateways of a specific type in the BPMN XML
     */
    private void findGatewaysByType(Document document, String gatewayType, String gatewayName, List<Map<String, Object>> gateways) {
        NodeList gatewayNodes = document.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", gatewayType);
        
        for (int i = 0; i < gatewayNodes.getLength(); i++) {
            Element gateway = (Element) gatewayNodes.item(i);
            String id = gateway.getAttribute("id");
            String name = gateway.getAttribute("name");
            
            if (name == null || name.isEmpty()) {
                name = gatewayName + " " + (i + 1);
            }
            
            Map<String, Object> gatewayInfo = new HashMap<>();
            gatewayInfo.put("id", id);
            gatewayInfo.put("name", name);
            gatewayInfo.put("type", gatewayType);
            
            // Find outgoing sequence flows
            NodeList outgoing = gateway.getElementsByTagNameNS("http://www.omg.org/spec/BPMN/20100524/MODEL", "outgoing");
            List<String> outgoingFlows = new ArrayList<>();
            
            for (int j = 0; j < outgoing.getLength(); j++) {
                outgoingFlows.add(outgoing.item(j).getTextContent());
            }
            
            gatewayInfo.put("outgoingFlows", outgoingFlows);
            gateways.add(gatewayInfo);
        }
    }
    
    /**
     * Enrich gateway information with activity instance data
     */
    private void enrichGatewaysWithInstanceInfo(List<Map<String, Object>> gateways, Map<String, Object> activityInstances) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> childActivityInstances = (List<Map<String, Object>>) activityInstances.get("childActivityInstances");
        
        for (Map<String, Object> gateway : gateways) {
            String gatewayId = (String) gateway.get("id");
            
            // Find activity instance for this gateway
            for (Map<String, Object> activityInstance : childActivityInstances) {
                String activityId = (String) activityInstance.get("activityId");
                
                if (gatewayId.equals(activityId)) {
                    gateway.put("activityInstance", activityInstance);
                    gateway.put("active", true);
                    break;
                }
            }
            
            // If no activity instance found, gateway is not active
            if (!gateway.containsKey("active")) {
                gateway.put("active", false);
            }
        }
    }
    
    /**
     * Add cancel instructions for a gateway
     */
    private void addCancelInstructionsForGateway(List<Map<String, Object>> instructions, Map<String, Object> activityInstances, String gatewayId) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> childActivityInstances = (List<Map<String, Object>>) activityInstances.get("childActivityInstances");
        
        for (Map<String, Object> activityInstance : childActivityInstances) {
            String activityId = (String) activityInstance.get("activityId");
            
            if (gatewayId.equals(activityId)) {
                String activityInstanceId = (String) activityInstance.get("id");
                
                Map<String, Object> cancelInstruction = new HashMap<>();
                cancelInstruction.put("type", "cancel");
                cancelInstruction.put("activityInstanceId", activityInstanceId);
                
                instructions.add(cancelInstruction);
                break;
            }
        }
    }
    
    /**
     * Format variables for process instance modification
     */
    private Map<String, Object> formatVariablesForModification(Map<String, Object> variables) {
        Map<String, Object> formattedVariables = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            Map<String, Object> variableInfo = new HashMap<>();
            variableInfo.put("value", entry.getValue());
            variableInfo.put("type", determineType(entry.getValue()));
            variableInfo.put("local", false);
            
            formattedVariables.put(entry.getKey(), variableInfo);
        }
        
        return formattedVariables;
    }
    
    /**
     * Determine variable type
     */
    private String determineType(Object value) {
        if (value instanceof String) return "String";
        if (value instanceof Integer) return "Integer";
        if (value instanceof Long) return "Long";
        if (value instanceof Double) return "Double";
        if (value instanceof Boolean) return "Boolean";
        if (value instanceof Map) return "Json";
        if (value instanceof List) return "Json";
        return "String"; // default type
    }
} 