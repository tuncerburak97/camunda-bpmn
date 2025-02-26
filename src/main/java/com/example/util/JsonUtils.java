package com.example.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Central JSON processing utility class.
 * All JSON conversion operations should be done through this class.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonUtils {
    private final ObjectMapper objectMapper;

    /**
     * Converts JSON string to specified class type.
     *
     * @param json JSON string
     * @param clazz Target class type
     * @param <T> Conversion type
     * @return Converted object
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            log.error("JSON to object conversion failed: {}", e.getMessage(), e);
            throw new RuntimeException("JSON conversion error", e);
        }
    }

    /**
     * Converts JSON string to specified TypeReference type.
     * Used for complex generic types (e.g. List<Map<String, Object>>)
     *
     * @param json JSON string
     * @param typeReference Target type reference
     * @param <T> Conversion type
     * @return Converted object
     */
    public <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (IOException e) {
            log.error("JSON to object conversion failed: {}", e.getMessage(), e);
            throw new RuntimeException("JSON conversion error", e);
        }
    }

    /**
     * Converts JSON string to Map<String, Object> type.
     *
     * @param json JSON string
     * @return Object converted as Map
     */
    public Map<String, Object> jsonToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            log.error("JSON to map conversion failed: {}", e.getMessage(), e);
            throw new RuntimeException("JSON conversion error", e);
        }
    }

    /**
     * Converts JSON string to Map<String, String> type.
     *
     * @param json JSON string
     * @return Object converted as String Map
     */
    public Map<String, String> jsonToStringMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            log.error("JSON to string map conversion failed: {}", e.getMessage(), e);
            throw new RuntimeException("JSON conversion error", e);
        }
    }

    /**
     * Converts object to JSON string.
     *
     * @param object Object to convert
     * @return JSON string
     */
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Object to JSON conversion failed: {}", e.getMessage(), e);
            throw new RuntimeException("JSON conversion error", e);
        }
    }

    /**
     * Converts JSON string to JsonNode.
     *
     * @param json JSON string
     * @return JsonNode
     */
    public JsonNode toJsonNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (IOException e) {
            log.error("JSON to JsonNode conversion failed: {}", e.getMessage(), e);
            throw new RuntimeException("JSON conversion error", e);
        }
    }

    /**
     * Converts an object to another type.
     * For example: Can be used for Map -> DTO or DTO -> Entity conversions.
     *
     * @param fromValue Source object
     * @param toValueType Target type
     * @param <T> Conversion type
     * @return Converted object
     */
    public <T> T convert(Object fromValue, Class<T> toValueType) {
        return objectMapper.convertValue(fromValue, toValueType);
    }

    /**
     * Converts an object to another complex type.
     *
     * @param fromValue Source object
     * @param toValueTypeRef Target type reference
     * @param <T> Conversion type
     * @return Converted object
     */
    public <T> T convert(Object fromValue, TypeReference<T> toValueTypeRef) {
        return objectMapper.convertValue(fromValue, toValueTypeRef);
    }

    /**
     * Validates JSON string against specified schema.
     *
     * @param json JSON string to validate
     * @param schema JSON schema string
     * @throws RuntimeException If validation fails
     */
    public void validateJsonSchema(String json, String schema) {
        try {
            com.networknt.schema.JsonSchemaFactory factory = 
                com.networknt.schema.JsonSchemaFactory.getInstance(com.networknt.schema.SpecVersion.VersionFlag.V4);
            com.networknt.schema.JsonSchema jsonSchema = factory.getSchema(schema);
            JsonNode jsonNode = toJsonNode(json);
            
            java.util.Set<com.networknt.schema.ValidationMessage> validationResult = jsonSchema.validate(jsonNode);
            if (!validationResult.isEmpty()) {
                throw new RuntimeException("JSON validation failed: " + validationResult);
            }
        } catch (Exception e) {
            log.error("Error validating JSON schema: {}", e.getMessage(), e);
            throw new RuntimeException("Error validating JSON", e);
        }
    }
} 