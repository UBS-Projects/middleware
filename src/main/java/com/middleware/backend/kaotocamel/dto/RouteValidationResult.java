package com.middleware.backend.kaotocamel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteValidationResult {
    private boolean valid;
    private String errorMessage;
}
