package com.middleware.backend.audit_logs_interceptor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for {@link AuditLog}.
 * This class is used to transfer audit log data between different layers of the application,
 * particularly for client-facing responses, without exposing the JPA entity directly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditDTO {
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
    /** The headers of the HTTP request. */
    private String requestHeaders;
    /** The body of the HTTP request. */
    private String requestBody;
    /** The headers of the HTTP response. */
    private String responseHeaders;
    /** The body of the HTTP response. */
    private String responseBody;
}
