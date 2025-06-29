package com.middleware.backend.kaotocamel.dto;

import lombok.Data;

@Data
public class RouteTestRequest {
    private String yamlContent;
    private String testMessage; // Optional test input
}
