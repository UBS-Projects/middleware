package com.middleware.backend.logging.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepLogDto {
    private Long id;
    private String transactionId;
    private Long workflowExecutionId;
    private Long stepId;
    private String executionStatus;
    private String requestPayload;
    private String responsePayload;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
