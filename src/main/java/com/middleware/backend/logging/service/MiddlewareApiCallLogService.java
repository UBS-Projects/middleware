package com.middleware.backend.logging.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.http.common.HttpMessage;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.middleware.backend.logging.model.MiddlewareApiCallLog;
import com.middleware.backend.logging.repository.MiddlewareApiCallLogRepository;
import com.middleware.backend.users.config.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MiddlewareApiCallLogService {

    private final MiddlewareApiCallLogRepository callLogRepository;
    private final JwtUtil jwtUtil;

    // RFC-4122 UUID validation pattern
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    );

    @Transactional
    public Long createTransactionSync(String routeId, Exchange exchange) {
        log.debug("createTransactionSync... creating log for Route [{}] - Exchange content: {}", routeId,
                exchange.getAllProperties());

        String sourceTransactionUUID = exchange.getIn().getHeader("transactionUUID", String.class);
        if (sourceTransactionUUID == null) {
            sourceTransactionUUID = exchange.getIn().getHeader("X-Transaction-UUID", String.class);
        }
        if (sourceTransactionUUID == null) {
            sourceTransactionUUID = exchange.getProperty("transactionUUID", String.class);
        }

        if (sourceTransactionUUID == null || sourceTransactionUUID.trim().isEmpty()) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
            exchange.getIn().setBody("{\"status\":\"ERROR\",\"message\":\"Missing required query parameter: transactionUUID\"}");
            throw new IllegalArgumentException("Missing required query parameter: transactionUUID");
        }

        sourceTransactionUUID = sourceTransactionUUID.trim().toLowerCase();
        if (!UUID_PATTERN.matcher(sourceTransactionUUID).matches()) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
            exchange.getIn().setBody("{\"status\":\"ERROR\",\"message\":\"Invalid transactionUUID format. Expect RFC4122 (e.g., 550e8400-e29b-41d4-a716-446655440000)\"}");
            throw new IllegalArgumentException("Invalid transactionUUID format");
        }

        if (callLogRepository.existsBySourceTransactionUUID(sourceTransactionUUID)) {
            log.error("Duplicate transactionUUID: {}", sourceTransactionUUID);
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 409);
            exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
            exchange.getIn().setBody("{\"status\":\"CONFLICT\",\"message\":\"Duplicate transactionUUID. This request was already processed.\"}");
            throw new IllegalArgumentException("Duplicate transactionUUID. This UUID has already been used.");
        }

        exchange.getIn().setHeader("X-Transaction-UUID", sourceTransactionUUID);

        // Extract client IP
        String clientIp = extractClientIp(exchange);

        // Extract user email from JWT token
        String userEmail = extractUserFromToken(exchange);

        MiddlewareApiCallLog logEntity = MiddlewareApiCallLog.builder()
                .routeId(routeId)
                .apiEndpoint(exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : null)
                .requestMethod(exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class))
                .requestHeaders(exchange.getIn().getHeaders() != null ? exchange.getIn().getHeaders().toString() : null)
                .requestBody(safeBodyString(exchange))
                .receivedAt(java.time.LocalDateTime.now())
                .transactionId(UUID.randomUUID().toString())
                .sourceTransactionUUID(sourceTransactionUUID)
                .status("IN_PROGRESS")
                .clientIp(clientIp)
                .userId(userEmail) // Store user email in userId field
                .build();

        MiddlewareApiCallLog saved = callLogRepository.save(logEntity);
        exchange.setProperty("apiLogId", saved.getId());
        exchange.setProperty("transactionUUID", sourceTransactionUUID);
        return saved.getId();
    }

    /**
     * Extract client IP from various possible headers and sources
     */
    private String extractClientIp(Exchange exchange) {
        try {
            // Try to get HTTP request from exchange
            HttpServletRequest request = exchange.getIn().getHeader(Exchange.HTTP_SERVLET_REQUEST, HttpServletRequest.class);
            if (request != null) {
                return getClientIpFromRequest(request);
            }

            // Fallback to headers if no servlet request
            String ip = exchange.getIn().getHeader("X-Forwarded-For", String.class);
            if (ip != null && !ip.isEmpty() && !ip.equalsIgnoreCase("unknown")) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                return ip.split(",")[0].trim();
            }

            ip = exchange.getIn().getHeader("X-Real-IP", String.class);
            if (ip != null && !ip.isEmpty() && !ip.equalsIgnoreCase("unknown")) {
                return ip;
            }

            ip = exchange.getIn().getHeader("CF-Connecting-IP", String.class); // Cloudflare
            if (ip != null && !ip.isEmpty() && !ip.equalsIgnoreCase("unknown")) {
                return ip;
            }

            ip = exchange.getIn().getHeader("X-Cluster-Client-IP", String.class);
            if (ip != null && !ip.isEmpty() && !ip.equalsIgnoreCase("unknown")) {
                return ip;
            }

            // Try to get from Camel HTTP component
            if (exchange.getIn() instanceof HttpMessage) {
                HttpServletRequest httpRequest = ((HttpMessage) exchange.getIn()).getRequest();
                if (httpRequest != null) {
                    return getClientIpFromRequest(httpRequest);
                }
            }

            log.debug("Could not extract client IP from exchange headers");
            return "Unknown";

        } catch (Exception e) {
            log.error("Error extracting client IP: {}", e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Extract client IP from HttpServletRequest with various fallbacks
     */
    private String getClientIpFromRequest(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !ip.equalsIgnoreCase("unknown")) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !ip.equalsIgnoreCase("unknown")) {
            return ip;
        }

        ip = request.getHeader("CF-Connecting-IP"); // Cloudflare
        if (ip != null && !ip.isEmpty() && !ip.equalsIgnoreCase("unknown")) {
            return ip;
        }

        ip = request.getHeader("X-Cluster-Client-IP");
        if (ip != null && !ip.isEmpty() && !ip.equalsIgnoreCase("unknown")) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !ip.equalsIgnoreCase("unknown")) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !ip.equalsIgnoreCase("unknown")) {
            return ip;
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (ip != null && !ip.isEmpty() && !ip.equalsIgnoreCase("unknown")) {
            return ip;
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip != null && !ip.isEmpty() && !ip.equalsIgnoreCase("unknown")) {
            return ip;
        }

        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Extract user email from JWT token in Authorization header
     */
    private String extractUserFromToken(Exchange exchange) {
        try {
            String authHeader = exchange.getIn().getHeader("Authorization", String.class);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return jwtUtil.extractEmail(token);
            }

            log.debug("No Authorization header or invalid format found");
            return "Anonymous";

        } catch (Exception e) {
            log.error("Error extracting user from token: {}", e.getMessage());
            return "Anonymous";
        }
    }

    private String safeBodyString(Exchange exchange) {
        try {
            Object body = exchange.getIn().getBody();
            if (body == null) return null;
            if (body instanceof byte[]) return new String((byte[]) body, java.nio.charset.StandardCharsets.UTF_8);
            return body.toString();
        } catch (Exception e) {
            log.warn("Error converting body to string: {}", e.getMessage());
            return null;
        }
    }

    @Async
    @Transactional
    public CompletableFuture<Void> updateTransaction(Exchange exchange) {
        log.debug("updateTransaction... updating Route [{}] - Exchange content: {}",
                exchange.getFromEndpoint().getEndpointUri(), exchange.getAllProperties());

        Long id = exchange.getProperty("apiLogId", Long.class);
        if (id == null || id <= 0) {
            log.warn("No valid apiLogId found for transaction update, skipping");
            return CompletableFuture.completedFuture(null);
        }

        try {
            MiddlewareApiCallLog existing = callLogRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Log not found to update with ID: " + id));

            log.debug("Updating transaction log with ID: {} routeId {} and transactionId {}",
                    id, existing.getRouteId(), existing.getTransactionId());

            // Set completion time first
            existing.setCompletedAt(java.time.LocalDateTime.now());
            existing.setDurationMs(
                    java.time.Duration.between(existing.getReceivedAt(), existing.getCompletedAt()).toMillis());

            if (exchange.isFailed()) {
                Exception ex = exchange.getException();
                log.error("updateTransaction() Route [{}] - Failed exchange: {} - Exception: {}",
                        existing.getRouteId(), existing.getTransactionId(), ex != null ? ex.getMessage() : "Unknown");

                existing.setStatus("FAILED");

                // Enhanced error handling
                if (ex != null) {
                    existing.setErrorMessage(ex.getMessage());

                    // Try to extract more specific error details
                    String fullErrorMessage = extractFullErrorMessage(ex);
                    if (fullErrorMessage.length() > ex.getMessage().length()) {
                        existing.setErrorMessage(fullErrorMessage);
                    }
                }

                // Get response code - try multiple sources
                Integer responseCode = extractResponseCode(exchange);
                existing.setResponseCode(responseCode);

                // Set response headers and body even for failures
                existing.setResponseHeaders(safeHeadersString(exchange));
                existing.setResponseBody(safeResponseBodyString(exchange));

                log.error("Transaction {} failed with response code: {}, error: {}",
                        existing.getTransactionId(), responseCode, existing.getErrorMessage());

            } else {
                log.info("updateTransaction() Route [{}] - Completed exchange: {}",
                        existing.getRouteId(), exchange.getExchangeId());

                existing.setStatus("COMPLETED");

                // Get response code
                Integer responseCode = extractResponseCode(exchange);
                existing.setResponseCode(responseCode);

                existing.setResponseHeaders(safeHeadersString(exchange));
                existing.setResponseBody(safeResponseBodyString(exchange));

                log.info("Transaction {} completed successfully with response code: {}",
                        existing.getTransactionId(), responseCode);
            }

            callLogRepository.save(existing);
            log.info("Transaction {} for routeId: {} updated successfully in database",
                    existing.getTransactionId(), existing.getRouteId());

        } catch (Exception e) {
            log.error("Error updating transaction with ID {}: {}", id, e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Extract response code from various possible sources in the exchange
     */
    private Integer extractResponseCode(Exchange exchange) {
        // Try different header names and sources
        Integer responseCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (responseCode != null) {
            return responseCode;
        }

        responseCode = exchange.getOut().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (responseCode != null) {
            return responseCode;
        }

        // Try message headers
        responseCode = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (responseCode != null) {
            return responseCode;
        }

        // If exchange failed but no response code set, default to 500
        if (exchange.isFailed()) {
            return 500;
        }

        // Default to 200 if no specific code found and no failure
        return 200;
    }

    /**
     * Extract complete error message including cause chain
     */
    private String extractFullErrorMessage(Exception ex) {
        if (ex == null) return "Unknown error";

        StringBuilder errorMsg = new StringBuilder(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());

        Throwable cause = ex.getCause();
        int depth = 0;
        while (cause != null && depth < 3) { // Limit depth to avoid very long messages
            errorMsg.append(" -> ").append(cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName());
            cause = cause.getCause();
            depth++;
        }

        return errorMsg.toString();
    }

    /**
     * Safely convert headers to string
     */
    private String safeHeadersString(Exchange exchange) {
        try {
            if (exchange.getIn().getHeaders() != null) {
                return exchange.getIn().getHeaders().toString();
            }
            return null;
        } catch (Exception e) {
            log.warn("Error converting headers to string: {}", e.getMessage());
            return "Error reading headers";
        }
    }

    /**
     * Safely convert response body to string
     */
    private String safeResponseBodyString(Exchange exchange) {
        try {
            Object body = exchange.getIn().getBody();
            if (body == null) {
                body = exchange.getOut().getBody();
            }
            if (body == null) {
                body = exchange.getMessage().getBody();
            }

            if (body == null) return null;
            if (body instanceof byte[]) return new String((byte[]) body, java.nio.charset.StandardCharsets.UTF_8);
            return body.toString();
        } catch (Exception e) {
            log.warn("Error converting response body to string: {}", e.getMessage());
            return "Error reading response body";
        }
    }
}