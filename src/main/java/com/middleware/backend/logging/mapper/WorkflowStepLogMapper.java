package com.middleware.backend.logging.mapper;

import com.middleware.backend.logging.dto.WorkflowStepLogDto;
import com.middleware.backend.logging.model.WorkflowStepLog;
import org.springframework.stereotype.Component;

@Component
public class WorkflowStepLogMapper {
    public WorkflowStepLogDto toDto(WorkflowStepLog log) {
        return WorkflowStepLogDto.builder().id(log.getId()).transactionId(log.getTransactionId())
                .workflowExecutionId(log.getWorkflowExecutionId()).stepId(log.getStepId())
                .executionStatus(log.getExecutionStatus()).requestPayload(log.getRequestPayload())
                .responsePayload(log.getResponsePayload()).errorMessage(log.getErrorMessage())
                .retryCount(log.getRetryCount()).startedAt(log.getStartedAt()).endedAt(log.getEndedAt()).build();
    }
}
