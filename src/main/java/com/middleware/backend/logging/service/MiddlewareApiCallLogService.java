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


/*
@Transactional
    public Long createTransactionSync(String routeId, Exchange exchange) {
        log.debug("createTransactionSync... creating log for Route [{}] - Exchange content: {}", routeId,
                exchange.getAllProperties());

        String userEmail = "anonymous";

        // Option 1: From Spring Security context (if Camel route runs in secured thread)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("**************************************");
        System.out.println(auth);
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            // Use "email" claim or "sub"
            System.out.println("**************************************");
            System.out.println(jwtAuth);
            System.out.println("**************************************");
            System.out.println(jwtAuth.getToken());
            System.out.println();
            System.out.println("**************************************");
            userEmail = jwtAuth.getToken().getClaimAsString("email");
            if (userEmail == null) {
                userEmail = jwtAuth.getName(); // fallback to sub
            }
        }

        // Option 2: Directly decode from Authorization header if above is empty
        if ("anonymous".equals(userEmail)) {
            String authHeader = exchange.getIn().getHeader("Authorization", String.class);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    com.nimbusds.jwt.JWT jwt = com.nimbusds.jwt.JWTParser.parse(token);
                    userEmail = jwt.getJWTClaimsSet().getStringClaim("email");
                    if (userEmail == null) {
                        userEmail = jwt.getJWTClaimsSet().getSubject();
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse JWT for user email", e);
                }
            }
        }

        MiddlewareApiCallLog logEntity = MiddlewareApiCallLog.builder()
                .transactionId(UUID.randomUUID().toString())
                .routeId(routeId)
                .apiEndpoint(exchange.getFromEndpoint().getEndpointUri())
                .requestMethod(exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class))
                .requestHeaders(exchange.getIn().getHeaders().toString())
                .requestBody(exchange.getIn().getBody(String.class))
                .receivedAt(java.time.LocalDateTime.now())
                .status("IN_PROGRESS")
                .clientIp(userEmail) // 👈 add this field to your entity
                .build();

        MiddlewareApiCallLog saved = callLogRepository.save(logEntity);
        return saved.getId();
    }


* */
