package com.middleware.backend.logging.service;

import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.middleware.backend.logging.dto.MiddlewareApiCallLogDto;
import com.middleware.backend.logging.model.MiddlewareApiCallLog;
import com.middleware.backend.logging.repository.MiddlewareApiCallLogRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MiddlewareApiCallLogService {

    private final MiddlewareApiCallLogRepository callLogRepository;

    @Async
    @Transactional
    public CompletableFuture<Long> createTransaction(MiddlewareApiCallLogDto dto) {
        MiddlewareApiCallLog log = MiddlewareApiCallLog.builder().apiEndpointId(dto.getApiEndpointId())
                .workflowId(dto.getWorkflowId()).requestMethod(dto.getRequestMethod()).requestUri(dto.getRequestUri())
                .requestHeaders(dto.getRequestHeaders()).requestBody(dto.getRequestBody())
                .responseCode(dto.getResponseCode()).responseHeaders(dto.getResponseHeaders())
                .responseBody(dto.getResponseBody()).receivedAt(dto.getReceivedAt()).completedAt(dto.getCompletedAt())
                .durationMs(dto.getDurationMs()).clientIp(dto.getClientIp()).apiKeyId(dto.getApiKeyId())
                .userId(dto.getUserId()).throttlingApplied(dto.getThrottlingApplied())
                .errorMessage(dto.getErrorMessage()).correlationId(dto.getCorrelationId())
                .retryCount(dto.getRetryCount()).transactionId(dto.getTransactionId()).build();

        MiddlewareApiCallLog saved = callLogRepository.save(log);
        return CompletableFuture.completedFuture(saved.getId());
    }

    @Async
    @Transactional
    public CompletableFuture<Void> updateTransaction(Long id, MiddlewareApiCallLogDto dto) {
        MiddlewareApiCallLog log = callLogRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Log not found"));

        log.setWorkflowId(dto.getWorkflowId());
        log.setResponseCode(dto.getResponseCode());
        log.setResponseHeaders(dto.getResponseHeaders());
        log.setResponseBody(dto.getResponseBody());
        log.setCompletedAt(dto.getCompletedAt());
        log.setDurationMs(dto.getDurationMs());
        log.setThrottlingApplied(dto.getThrottlingApplied());
        log.setErrorMessage(dto.getErrorMessage());
        log.setRetryCount(dto.getRetryCount());

        callLogRepository.save(log);
        return CompletableFuture.completedFuture(null);
    }
}
