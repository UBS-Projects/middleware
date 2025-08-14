package com.middleware.backend.audit_logs_interceptor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
