package com.middleware.backend.audit_logs_interceptor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A response object that provides a summarized view of an {@link AuditLog}.
 * This class is typically used in API responses where a concise representation of an audit log is needed,
 * omitting verbose details like headers and bodies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditResponse {
    /** The unique identifier of the audit log entry. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /** The username of the user who performed the action. */
    private String userName;
    /** The HTTP method of the request (e.g., GET, POST). */
    private String method;
    /** The API path that was accessed. */
    private String apiPath;
    /** The HTTP status code of the response. */
    private Integer responseStatus;
    /** The timestamp when the request processing started. */
    private LocalDateTime startTime;
    /** The timestamp when the request processing ended. */
    private LocalDateTime endTime;
    /** The duration of the request processing in milliseconds. */
    private Long durationMs;
}
