package com.middleware.backend.logging.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("user_email")
    private String userId;

    private String errorMessage;
    private Integer retryCount;
    private String sourceTransactionUUID;
}