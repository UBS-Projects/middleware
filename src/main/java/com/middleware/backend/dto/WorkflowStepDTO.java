package com.middleware.backend.dto;

import java.time.LocalDateTime;

import lombok.Data;


@Data
public class WorkflowStepDTO {

    private Long id;
    private Long workflowConfigId;   // Reference to workflow_config
    private Long parentStepId;        // Reference to parent step (for fork/join)
    private Integer stepOrder;        // Order of execution inside the parent
    private String stepName;          // Friendly name
    private String stepType;          // API_CALL, TRANSFORMATION, DELAY, etc.
    private Long destinationApiId;    // Linked API if step type is API_CALL

    private String transformationExpression; // SpEL Expression for transformations
    private Integer delaySeconds;             // For delay steps
    private String conditionExpression;       // SpEL for condition steps
    private Integer retryCount;                // For retry steps
    private Integer retryDelaySeconds;         // Milliseconds between retries
    private Long forkGroupId;                  // Fork/Join grouping for parallel steps

    private Long createdBy;         // User who created this step
    private Long updatedBy;         // User who updated
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
