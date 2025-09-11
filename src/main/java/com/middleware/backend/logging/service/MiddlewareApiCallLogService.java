package com.middleware.backend.logging.service;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.http.common.HttpMessage;
import org.apache.camel.util.ObjectHelper;
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
            setError(exchange, 400, "{\"status\":\"ERROR\",\"message\":\"Missing required query parameter: transactionUUID\"}");
            throw new IllegalArgumentException("Missing required query parameter: transactionUUID");
        }
        sourceTransactionUUID = sourceTransactionUUID.trim().toLowerCase();
        if (!UUID_PATTERN.matcher(sourceTransactionUUID).matches()) {
            setError(exchange, 400, "{\"status\":\"ERROR\",\"message\":\"Invalid transactionUUID format. Expect RFC4122 (e.g., 550e8400-e29b-41d4-a716-446655440000)\"}");
            throw new IllegalArgumentException("Invalid transactionUUID format");
        }

        boolean isRetry = "true".equalsIgnoreCase(exchange.getIn().getHeader("X-Retry-Attempt", String.class));
        int attemptNo;
        int retryCount;
        if (isRetry) {
            MiddlewareApiCallLog last = callLogRepository.findTopBySourceTransactionUUIDOrderByAttemptNoDesc(sourceTransactionUUID);
            attemptNo = (last == null) ? 1 : last.getAttemptNo() + 1;
            retryCount = Math.max(0, attemptNo - 1);
        } else {
            if (callLogRepository.existsBySourceTransactionUUID(sourceTransactionUUID)) {
                setError(exchange, 409, "{\"status\":\"CONFLICT\",\"message\":\"Duplicate transactionUUID. This request was already processed.\"}");
                throw new IllegalArgumentException("Duplicate transactionUUID. This UUID has already been used.");
            }
            attemptNo = 1;
            retryCount = 0;
        }

        exchange.getIn().setHeader("X-Transaction-UUID", sourceTransactionUUID);

        String clientIp = extractClientIp(exchange);
        String userEmail = extractUserFromToken(exchange);
        String requestBody = readBodyAsString(exchange.getIn());
        String requestUrl = header(exchange, "CamelHttpUrl", String.class);
        String requestPath = header(exchange, "CamelHttpPath", String.class);
        String requestQuery = header(exchange, "CamelHttpQuery", String.class);

        MiddlewareApiCallLog logEntity = MiddlewareApiCallLog.builder()
                .routeId(routeId)
                .apiEndpoint(exchange.getFromEndpoint() != null ? exchange.getFromEndpoint().getEndpointUri() : null)
                .requestMethod(exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class))
                .requestUrl(requestUrl)
                .requestPath(requestPath)
                .requestQuery(requestQuery)
                .requestHeaders(exchange.getIn().getHeaders() != null ? exchange.getIn().getHeaders().toString() : null)
                .requestBody(requestBody)
                .receivedAt(java.time.LocalDateTime.now())
                .transactionId(UUID.randomUUID().toString())
                .sourceTransactionUUID(sourceTransactionUUID)
                .attemptNo(attemptNo)
                .retryCount(retryCount)
                .status("IN_PROGRESS")
                .clientIp(clientIp)
                .userId(userEmail)
                .build();

        MiddlewareApiCallLog saved = callLogRepository.save(logEntity);
        exchange.setProperty("apiLogId", saved.getId());
        exchange.setProperty("transactionUUID", sourceTransactionUUID);
        return saved.getId();
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

            existing.setCompletedAt(java.time.LocalDateTime.now());
            existing.setDurationMs(
                    java.time.Duration.between(existing.getReceivedAt(), existing.getCompletedAt()).toMillis());

            Integer responseCode = extractResponseCode(exchange);
            existing.setResponseCode(responseCode);
            existing.setResponseHeaders(safeHeadersString(exchange));
            existing.setResponseBody(readResponseBodyAsString(exchange));

            boolean isSpecialApi = isSpecialIntegrateApi(existing);
            boolean isDryRunFalse = isDryRunFalse(existing);

            if (isSpecialApi && isDryRunFalse && responseCode != null && responseCode == 409) {
                log.info("Special case: API {} with dry-run=false and 409 response - treating as 200 OK",
                        existing.getRequestUrl());
                existing.setResponseCode(200);
                existing.setErrorMessage(null);
                responseCode = 200;
            } else {
                Exception ex = exchange.getException();
                if (ex != null) {
                    String fullErrorMessage = extractFullErrorMessage(ex);
                    existing.setErrorMessage(ObjectHelper.isEmpty(fullErrorMessage) ? ex.getMessage() : fullErrorMessage);
                } else if (responseCode != null && responseCode >= 400) {
                    existing.setErrorMessage("HTTP " + responseCode + " - " +
                            (existing.getResponseBody() != null ? existing.getResponseBody() : "No error details"));
                } else {
                    existing.setErrorMessage(null);
                }
            }

            existing.setStatus("COMPLETED");

            callLogRepository.save(existing);
            log.info("Transaction {} for routeId: {} updated successfully: status=COMPLETED, responseCode={}",
                    existing.getTransactionId(), existing.getRouteId(), existing.getResponseCode());

        } catch (Exception e) {
            log.error("Failed to update transaction with ID {}: {}", id, e.getMessage(), e);

            try {
                MiddlewareApiCallLog existing = callLogRepository.findById(id).orElse(null);
                if (existing != null) {
                    existing.setStatus("FAILED");
                    existing.setErrorMessage("Failed to update log: " + e.getMessage());
                    existing.setCompletedAt(java.time.LocalDateTime.now());
                    if (existing.getReceivedAt() != null) {
                        existing.setDurationMs(
                                java.time.Duration.between(existing.getReceivedAt(), existing.getCompletedAt()).toMillis());
                    }
                    callLogRepository.save(existing);
                }
            } catch (Exception saveEx) {
                log.error("Failed to update transaction status to FAILED for ID {}: {}", id, saveEx.getMessage());
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    private <T> T header(Exchange exchange, String name, Class<T> type) {
        try {
            return exchange.getIn().getHeader(name, type);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isSpecialIntegrateApi(MiddlewareApiCallLog log) {
        String url = log.getRequestUrl();
        String path = log.getRequestPath();

        if (url != null && url.contains("/camel/v1/datasets")) {
            return true;
        }

        if (path != null && path.contains("/camel/v1/datasets")) {
            return true;
        }

        String apiEndpoint = log.getApiEndpoint();
        if (apiEndpoint != null && apiEndpoint.contains("/camel/v1/datasets")) {
            return true;
        }

        return false;
    }


    private boolean isDryRunFalse(MiddlewareApiCallLog log) {
        String query = log.getRequestQuery();
        String url = log.getRequestUrl();
        String path = log.getRequestPath();

        if (query != null && query.contains("dry-run=false")) {
            return true;
        }

        if (url != null && url.contains("dry-run=false")) {
            return true;
        }

        if (path != null && path.contains("dry-run=false")) {
            return true;
        }

        return false;
    }
    private void setError(Exchange exchange, int status, String json) {
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, status);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getIn().setBody(json);
    }

    private String extractClientIp(Exchange exchange) {
        try {
            HttpServletRequest request = exchange.getIn()
                    .getHeader(Exchange.HTTP_SERVLET_REQUEST, HttpServletRequest.class);
            if (request != null) {
                return getClientIpFromRequest(request);
            }

            String ip = exchange.getIn().getHeader("X-Forwarded-For", String.class);
            if (!isUnknown(ip)) return ip.split(",")[0].trim();

            ip = exchange.getIn().getHeader("X-Real-IP", String.class);
            if (!isUnknown(ip)) return ip;

            ip = exchange.getIn().getHeader("CF-Connecting-IP", String.class);
            if (!isUnknown(ip)) return ip;

            ip = exchange.getIn().getHeader("X-Cluster-Client-IP", String.class);
            if (!isUnknown(ip)) return ip;

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

    private boolean isUnknown(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
    }

    private String getClientIpFromRequest(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!isUnknown(ip)) return ip.split(",")[0].trim();

        ip = request.getHeader("X-Real-IP");
        if (!isUnknown(ip)) return ip;

        ip = request.getHeader("CF-Connecting-IP");
        if (!isUnknown(ip)) return ip;

        ip = request.getHeader("X-Cluster-Client-IP");
        if (!isUnknown(ip)) return ip;

        ip = request.getHeader("Proxy-Client-IP");
        if (!isUnknown(ip)) return ip;

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (!isUnknown(ip)) return ip;

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (!isUnknown(ip)) return ip;

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (!isUnknown(ip)) return ip;

        return request.getRemoteAddr();
    }

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

    private String readBodyAsString(Message msg) {
        try {
            String contentType = msg.getHeader(Exchange.CONTENT_TYPE, String.class);
            Charset cs = StandardCharsets.UTF_8;
            if (contentType != null) {
                String lower = contentType.toLowerCase();
                int i = lower.indexOf("charset=");
                if (i >= 0) {
                    String enc = lower.substring(i + "charset=".length()).trim();
                    int semi = enc.indexOf(';');
                    if (semi > 0) enc = enc.substring(0, semi).trim();
                    try {
                        cs = Charset.forName(enc);
                    } catch (Exception ignore) {}
                }
            }

            String body = msg.getBody(String.class);
            if (body == null) return null;

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            return new String(bytes, cs);
        } catch (Exception e) {
            log.warn("Error reading request body as string: {}", e.getMessage());
            Object body = msg.getBody();
            if (body == null) return null;
            if (body instanceof byte[]) return new String((byte[]) body, StandardCharsets.UTF_8);
            return String.valueOf(body);
        }
    }

    private Integer extractResponseCode(Exchange exchange) {
        Integer responseCode = exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (responseCode != null) return responseCode;

        if (exchange.getOut() != null) {
            responseCode = exchange.getOut().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
            if (responseCode != null) return responseCode;
        }

        responseCode = exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
        if (responseCode != null) return responseCode;

        return exchange.isFailed() ? 500 : 200;
    }

    private String extractFullErrorMessage(Exception ex) {
        if (ex == null) return "Unknown error";
        StringBuilder errorMsg = new StringBuilder(
                ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        Throwable cause = ex.getCause();
        int depth = 0;
        while (cause != null && depth < 3) {
            errorMsg.append(" -> ").append(
                    cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName());
            cause = cause.getCause();
            depth++;
        }
        return errorMsg.toString();
    }

    private String safeHeadersString(Exchange exchange) {
        try {
            if (exchange.getMessage() != null && exchange.getMessage().getHeaders() != null) {
                return exchange.getMessage().getHeaders().toString();
            }
            if (exchange.getIn() != null && exchange.getIn().getHeaders() != null) {
                return exchange.getIn().getHeaders().toString();
            }
            return null;
        } catch (Exception e) {
            log.warn("Error converting headers to string: {}", e.getMessage());
            return "Error reading headers";
        }
    }

    private String readResponseBodyAsString(Exchange exchange) {
        try {
            Message m = exchange.getMessage();
            if (m != null) {
                String b = m.getBody(String.class);
                if (b != null) return b;
            }
            if (exchange.getOut() != null) {
                String b = exchange.getOut().getBody(String.class);
                if (b != null) return b;
            }
            if (exchange.getIn() != null) {
                String b = exchange.getIn().getBody(String.class);
                if (b != null) return b;
            }
            return null;
        } catch (Exception e) {
            log.warn("Error reading response body as string: {}", e.getMessage());
            Object body = exchange.getMessage() != null ? exchange.getMessage().getBody() : null;
            if (body == null && exchange.getOut() != null) body = exchange.getOut().getBody();
            if (body == null && exchange.getIn() != null) body = exchange.getIn().getBody();
            if (body == null) return null;
            if (body instanceof byte[]) return new String((byte[]) body, StandardCharsets.UTF_8);
            return String.valueOf(body);
        }
    }
}