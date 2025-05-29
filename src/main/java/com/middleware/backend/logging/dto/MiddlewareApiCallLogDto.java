package com.middleware.backend.logging.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiddlewareApiCallLogDto {
    private Long id;
    private String transactionId;
    private Long apiEndpointId;
    private Long workflowId;
    private String requestMethod;
    private String requestUri;
    private String requestHeaders;
    private String requestBody;
    private Integer responseCode;
    private String responseHeaders;
    private String responseBody;
    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private String clientIp;
    private Long apiKeyId;
    private Long userId;
    private Boolean throttlingApplied;
    private String errorMessage;
    private String correlationId;
    private Integer retryCount;
}
