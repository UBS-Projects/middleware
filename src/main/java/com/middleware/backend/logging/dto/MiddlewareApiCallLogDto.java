package com.middleware.backend.logging.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MiddlewareApiCallLogDto {
    private Long id;
    private String transactionId;
    private String apiEndpoint;
    private String routeId;
    private String requestMethod;
    private String requestHeaders;
    private String requestBody;
    private String status;
    private Integer responseCode;
    private String responseHeaders;
    private String responseBody;
    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private String clientIp;
    private Long apiKeyId;
    private Long userId;
    // private Boolean throttlingApplied;
    private String errorMessage;
    private Integer retryCount;
}
