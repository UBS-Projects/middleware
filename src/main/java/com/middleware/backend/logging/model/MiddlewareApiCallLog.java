package com.middleware.backend.logging.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

 @Entity
@Table(
        name = "middleware_api_call_log",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_call_uuid_attempt", columnNames = {"source_transaction_uuid", "attempt_no"}),
                @UniqueConstraint(name = "uk_transaction_id", columnNames = {"transaction_id"})
        }
)
@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor @Builder
public class MiddlewareApiCallLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 36)
    private String transactionId;

    @Column(name = "route_Id", nullable = false)
    private String routeId;

    @Column(name = "api_endpoint")
    private String apiEndpoint;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_url", length = 2048)
    private String requestUrl;

    @Column(name = "request_path", length = 1024)
    private String requestPath;

    @Column(name = "request_query", length = 2048)
    private String requestQuery;

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "response_headers", columnDefinition = "TEXT")
    private String responseHeaders;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "client_ip", length = 100)
    private String clientIp;

    @Column(name = "api_key_id")
    private Long apiKeyId;

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount;                

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo;

    @Column(name = "source_transaction_uuid", length = 36, nullable = false)
    private String sourceTransactionUUID;
}
