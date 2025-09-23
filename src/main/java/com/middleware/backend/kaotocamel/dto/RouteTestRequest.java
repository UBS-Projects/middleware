package com.middleware.backend.kaotocamel.dto;

import lombok.Data;

/**
 * Request payload for testing or validating a dynamic route definition.
 */
@Data
public class RouteTestRequest {
    /** YAML content representing one or more Camel routes. */
    private String yamlContent;
    /** Optional test input payload when running a route test. */
    private String testMessage; // Optional test input
}
