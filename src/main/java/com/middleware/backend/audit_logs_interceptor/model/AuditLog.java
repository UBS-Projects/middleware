package com.middleware.backend.audit_logs_interceptor.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Represents an audit log entry in the database.
 * This entity captures detailed information about an HTTP request and its corresponding response,
 * including timing, headers, and bodies, for auditing and debugging purposes.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    /** The unique identifier for the audit log entry. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The username of the user who initiated the request. Can be "anonymous". */
    private String userName;

    /** The HTTP method of the request (e.g., "GET", "POST"). */
    private String method;

    /** The URI path of the request. */
    private String apiPath;

    /** The query string of the request. */
    private String queryString;

    /** The HTTP status code of the response. */
    private Integer responseStatus;

    /** The timestamp when the request processing started. */
    private LocalDateTime startTime;

    /** The timestamp when the request processing ended. */
    private LocalDateTime endTime;

    /** The duration of the request processing in milliseconds. */
    private Long durationMs;

    /** The headers of the HTTP request, stored as a single text block. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestHeaders;

    /** The body of the HTTP request, stored as a single text block. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String requestBody;

    /** The headers of the HTTP response, stored as a single text block. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String responseHeaders;

    /** The body of the HTTP response, stored as a single text block. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String responseBody;
}
