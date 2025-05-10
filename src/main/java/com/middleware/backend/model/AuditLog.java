package com.middleware.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "step_id")
    private Long stepId;

    @Column(nullable = false, length = 50)
    private String status; // e.g., "SUCCESS" / "FAILURE"

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_context", columnDefinition = "TEXT")
    private String requestContext; // JSON snapshot of variables at time of execution

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs; // Milliseconds to complete step

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
