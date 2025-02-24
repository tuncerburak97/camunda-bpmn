package com.example.model.api.request;

import lombok.Data;

import java.util.List;

@Data
public class DeleteDeploymentRequest {
    private List<String> ids;
}
