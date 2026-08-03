package com.middleware.backend.camel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of validating a dynamic route YAML definition.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteValidationResult {
    /** True if the YAML definition is valid and loadable. */
    private boolean valid;
    /** Optional message describing why validation failed. */
    private String errorMessage;
}
