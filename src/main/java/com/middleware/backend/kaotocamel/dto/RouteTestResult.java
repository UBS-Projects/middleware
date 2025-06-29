package com.middleware.backend.kaotocamel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteTestResult {
    private boolean success;
    private String output;
    private String errorMessage;
}
