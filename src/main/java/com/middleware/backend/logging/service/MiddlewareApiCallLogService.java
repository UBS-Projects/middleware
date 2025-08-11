package com.middleware.backend.logging.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.camel.Exchange;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.middleware.backend.logging.model.MiddlewareApiCallLog;
import com.middleware.backend.logging.repository.MiddlewareApiCallLogRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MiddlewareApiCallLogService {

    private final MiddlewareApiCallLogRepository callLogRepository;

    @Transactional
    public Long createTransactionSync(String routeId, Exchange exchange) {
        log.debug("createTransactionSync... creating log for Route [{}] - Exchange content: {}", routeId,
                exchange.getAllProperties());

        MiddlewareApiCallLog log = MiddlewareApiCallLog.builder()
                .apiEndpoint(exchange.getFromEndpoint().getEndpointUri()).routeId(routeId)
                .apiEndpoint(exchange.getFromEndpoint().getEndpointUri())
                .requestMethod(exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class))
                .requestHeaders(exchange.getIn().getHeaders().toString())
                .requestBody(exchange.getIn().getBody(String.class)).receivedAt(java.time.LocalDateTime.now())
                .transactionId(UUID.randomUUID().toString()).status("IN_PROGRESS").build();

        MiddlewareApiCallLog saved = callLogRepository.save(log);
        return saved.getId();
    }

    @Async
    @Transactional
    public CompletableFuture<Void> updateTransaction(Exchange exchange) {
        log.debug("updateTransaction... updating  Route [{}] - Exchange content: {}",
                exchange.getFromEndpoint().getEndpointUri(), exchange.getAllProperties());
        Long id = exchange.getProperty("apiLogId", Long.class);
        MiddlewareApiCallLog existing = callLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Log not found to update"));
        log.debug("Updating transaction log with ID: {} routeId {} and transactionId {}", id, existing.getRouteId(),
                existing.getTransactionId());
        if (exchange.isFailed()) {
            Exception ex = exchange.getException();
            log.error("updateTransaction () Route [{}] - Failed exchange content: {} - Cause: {}",
                    existing.getTransactionId(), existing.getRouteId(), exchange.getAllProperties(), ex.getMessage());
            existing.setStatus("FAILED");
            existing.setCompletedAt(java.time.LocalDateTime.now());
            existing.setDurationMs(
                    java.time.Duration.between(existing.getReceivedAt(), existing.getCompletedAt()).toMillis());
            // Set error message if available
            existing.setErrorMessage(ex != null ? ex.getMessage() : "Unknown failure");
            existing.setResponseCode(exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class));

            existing.setResponseHeaders(exchange.getIn().getHeaders().toString());
            existing.setResponseBody(exchange.getIn().getBody(String.class));

        } else {
            log.info("update transaction {} Route [{}] - Completed exchange: {}", existing.getTransactionId(),
                    existing.getRouteId(), exchange.getExchangeId());
            existing.setStatus("COMPLETED");
            existing.setResponseCode(exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class));
            existing.setResponseHeaders(exchange.getIn().getHeaders().toString());
            existing.setResponseBody(exchange.getIn().getBody(String.class));
            existing.setCompletedAt(java.time.LocalDateTime.now());
            existing.setDurationMs(
                    java.time.Duration.between(existing.getReceivedAt(), existing.getCompletedAt()).toMillis());
        }
        callLogRepository.save(existing);
        log.info("updateTransaction function:  updated transaction {} for routeId: {} Database updated entity\n {}",
                existing.getTransactionId(), existing.getRouteId(), existing.toString());

        log.info("\n\nCurrent exchange content: {}", exchange.getAllProperties());

        return CompletableFuture.completedFuture(null);
    }

}
