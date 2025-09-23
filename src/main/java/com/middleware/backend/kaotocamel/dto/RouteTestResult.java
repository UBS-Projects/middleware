package com.middleware.backend.kaotocamel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result payload representing the outcome of a dynamic route test execution.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteTestResult {
    /** Whether the test execution completed successfully. */
    private boolean success;
    /** Optional output payload captured from the route under test. */
    private String output;
    /** Error details if the test failed. */
    private String errorMessage;
}
