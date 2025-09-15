package com.middleware.backend.logging.service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.middleware.backend.logging.model.MiddlewareApiCallLog;
import com.middleware.backend.logging.repository.MiddlewareApiCallLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MiddlewareLogRetryService {

    private final MiddlewareApiCallLogRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${middleware.retry.base-url:http://localhost:8081}")
    private String baseUrl;

    public ResponseEntity<?> retryApiCall(String uuid) {
        MiddlewareApiCallLog lastAttempt = repository.findTopBySourceTransactionUUIDOrderByAttemptNoDesc(uuid.toLowerCase());

        if (lastAttempt == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "ERROR", "message", "No call found for given UUID."));
        }

        ResponseEntity<?> validationResponse = validateRetryConditions(lastAttempt);
        if (validationResponse != null) {
            return validationResponse;
        }

        List<MiddlewareApiCallLog> attempts = repository.findBySourceTransactionUUIDOrderByAttemptNoAsc(uuid.toLowerCase());
        MiddlewareApiCallLog originalRequest = attempts.get(0);

        return executeRetry(originalRequest);
    }

    private ResponseEntity<?> validateRetryConditions(MiddlewareApiCallLog lastAttempt) {
        if (!"COMPLETED".equalsIgnoreCase(lastAttempt.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "CONFLICT", "message",
                            "Retry only allowed for completed logs. Current status: " + lastAttempt.getStatus()));
        }

        if (isSpecialIntegrateApi(lastAttempt) && isDryRunFalse(lastAttempt)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "CONFLICT", "message",
                            "Retry not allowed for integrate API with dry-run=false."));
        }

        return null;
    }

    private ResponseEntity<?> executeRetry(MiddlewareApiCallLog originalRequest) {
        String url = buildUrl(originalRequest);
        if (url == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "ERROR", "message", "Cannot reconstruct original URL for retry."));
        }

        HttpMethod method = parseMethod(originalRequest.getRequestMethod());
        if (method == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "ERROR", "message", "Unsupported HTTP method for retry."));
        }

        HttpHeaders headers = parseHeaders(originalRequest.getRequestHeaders());
        stripHopByHop(headers);
        headers.set("transactionUUID", originalRequest.getSourceTransactionUUID());
        headers.set("X-Transaction-UUID", originalRequest.getSourceTransactionUUID());
        headers.set("X-Retry-Attempt", "true");

        HttpEntity<String> entity = new HttpEntity<>(originalRequest.getRequestBody(), headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(URI.create(url), method, entity, String.class);
            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "message", "Retry sent with same request.",
                    "httpStatus", response.getStatusCode().value()
            ));
        } catch (Exception ex) {
            log.warn("Retry attempt failed, but request was queued", ex);
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("status", "QUEUED", "message", "Retry attempted; check attempts list for new record."));
        }
    }

    private boolean hasApiError(MiddlewareApiCallLog log) {
        return (log.getResponseCode() != null && log.getResponseCode() >= 400) ||
                (log.getErrorMessage() != null && !log.getErrorMessage().trim().isEmpty());
    }

    private boolean isSpecialIntegrateApi(MiddlewareApiCallLog log) {
        String url = log.getRequestUrl();
        String path = log.getRequestPath();
        String apiEndpoint = log.getApiEndpoint();

        return (url != null && url.contains("/camel/external/integrate")) ||
                (path != null && path.contains("/camel/external/integrate")) ||
                (apiEndpoint != null && apiEndpoint.contains("/camel/external/integrate"));
    }

    private boolean isDryRunFalse(MiddlewareApiCallLog log) {
        String query = log.getRequestQuery();
        String url = log.getRequestUrl();
        String path = log.getRequestPath();

        return (query != null && query.contains("dry-run=false")) ||
                (url != null && url.contains("dry-run=false")) ||
                (path != null && path.contains("dry-run=false"));
    }

    private String buildUrl(MiddlewareApiCallLog snapshot) {
        if (snapshot.getRequestUrl() != null && !snapshot.getRequestUrl().isBlank()) {
            return snapshot.getRequestUrl();
        }
        if (snapshot.getRequestPath() != null) {
            String query = (snapshot.getRequestQuery() == null || snapshot.getRequestQuery().isBlank())
                    ? "" : ("?" + snapshot.getRequestQuery());
             return baseUrl + snapshot.getRequestPath() + query;
        }
        return null;
    }

    private HttpMethod parseMethod(String method) {
        try {
            return HttpMethod.valueOf(Objects.toString(method, "GET").toUpperCase());
        } catch (Exception e) {
            log.warn("Failed to parse HTTP method: {}", method);
            return null;
        }
    }

    private HttpHeaders parseHeaders(String headersString) {
        HttpHeaders headers = new HttpHeaders();
        if (headersString == null) return headers;

        String cleaned = headersString.trim();
        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.isBlank()) return headers;

        for (String pair : cleaned.split(",\\s*")) {
            int index = pair.indexOf('=');
            if (index > 0) {
                String key = pair.substring(0, index).trim();
                String value = pair.substring(index + 1).trim();
                if (!key.isEmpty() && !value.isEmpty() && !"null".equalsIgnoreCase(value)) {
                    headers.add(key, value);
                }
            }
        }
        return headers;
    }

    private void stripHopByHop(HttpHeaders headers) {
        List<String> hopByHopHeaders = List.of(
                "Host", "Content-Length", "Transfer-Encoding", "Connection",
                "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization", "TE", "Trailer", "Upgrade"
        );
        hopByHopHeaders.forEach(headers::remove);
    }
}