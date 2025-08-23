package com.middleware.backend.audit_logs_interceptor.model;

import java.time.LocalDateTime;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;

@Data
@Builder

public class AuditResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userName;
    private String method;
    private String apiPath;
    private Integer responseStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
}
