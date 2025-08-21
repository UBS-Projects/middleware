package com.middleware.backend.audit_logs_interceptor.model;

import lombok.*;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogExport {
    private Long id;
    private String userName;
    private String method;
    private String apiPath;
    private String queryString;
    private Integer responseStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
}
