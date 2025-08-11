package com.middleware.backend.logging.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workflow_step_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id")
    private String transactionId;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "Api_CallLog", nullable = true)
    // @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private String middlewareApiCallLog;

    @Column(name = "workflow_execution_id", nullable = false)
    private Long workflowExecutionId;

    @Column(name = "step_id", nullable = false)
    private Long stepId;

    @Column(name = "execution_status", nullable = false)
    private String executionStatus;

    @Column(name = "request_payload", columnDefinition = "jsonb")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
