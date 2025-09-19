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
/**
 * DTO representing a snapshot of a middleware API call, used for responses/export.
 */
public class MiddlewareApiCallLogDto {
    /** Database primary key. */
    private Long id;
    /** Internal unique transaction identifier for this log row. */
    private String transactionId;
    /** Logical API endpoint or Camel endpoint URI. */
    private String apiEndpoint;
    /** Associated Camel route identifier. */
    private String routeId;
    /** HTTP method used by the request. */
    private String requestMethod;
    /** Full request URL if available. */
    private String requestUrl;
    /** Request path portion. */
    private String requestPath;
    /** Raw query string. */
    private String requestQuery;
    /** Request headers serialized to string. */
    private String requestHeaders;
    /** Request body serialized to string (may be large). */
    private String requestBody;
    /** Processing status (e.g., IN_PROGRESS, COMPLETED, FAILED). */
    private String status;
    /** HTTP response status code. */
    private Integer responseCode;
    /** Response headers serialized to string. */
    private String responseHeaders;
    /** Response body serialized to string (may be large). */
    private String responseBody;
    /** Timestamp when request was received. */
    private LocalDateTime receivedAt;
    /** Timestamp when processing completed. */
    private LocalDateTime completedAt;
    /** Processing duration in milliseconds. */
    private Long durationMs;
    /** Originating client IP if available. */
    private String clientIp;
    /** Optional API key identifier. */
    private Long apiKeyId;

    @JsonProperty("user_email")
    /** Email/identifier of the authenticated user associated with the request. */
    private String userId;

    /** Error details when applicable. */
    private String errorMessage;
    /** Number of retry attempts so far for this transaction. */
    private Integer retryCount;
    /** Attempt sequence number for the source transaction UUID. */
    private Integer attemptNo;
    /** Correlation id used by clients to group attempts. */
    private String sourceTransactionUUID;
}