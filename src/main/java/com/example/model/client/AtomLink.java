package com.example.model.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a HATEOAS link in the Camunda API responses
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AtomLink {
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("href")
    private String href;
    
    @JsonProperty("rel")
    private String rel;
} 