package com.middleware.backend.logging.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.middleware.backend.model.ApiEndpoint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "middleware_api_call_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiddlewareApiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "api_endpoint_id", nullable = false)
    private Long apiEndpointId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apiEndpoint", nullable = true)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private ApiEndpoint apiEndpoint;

    @Column(name = "workflow_id")
    private Long workflowId;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_uri", length = 500)
    private String requestUri;

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

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

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "throttling_applied")
    private Boolean throttlingApplied;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "retry_count")
    private Integer retryCount;
}
