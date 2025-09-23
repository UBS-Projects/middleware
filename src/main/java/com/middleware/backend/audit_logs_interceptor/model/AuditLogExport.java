package com.middleware.backend.audit_logs_interceptor.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a simplified version of an {@link AuditLog} for export purposes.
 * This class contains a subset of the fields from the main {@link AuditLog} entity,
 * intended for creating reports or exports (e.g., CSV, Excel) where full details like
 * request/response bodies and headers are not required.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogExport {
    /** The unique identifier of the audit log entry. */
    private Long id;
    /** The username of the user who performed the action. */
    private String userName;
    /** The HTTP method of the request (e.g., GET, POST). */
    private String method;
    /** The API path that was accessed. */
    private String apiPath;
    /** The query string of the request, if any. */
    private String queryString;
    /** The HTTP status code of the response. */
    private Integer responseStatus;
    /** The timestamp when the request processing started. */
    private LocalDateTime startTime;
    /** The timestamp when the request processing ended. */
    private LocalDateTime endTime;
    /** The duration of the request processing in milliseconds. */
    private Long durationMs;
}
