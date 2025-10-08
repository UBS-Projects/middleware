package com.middleware.backend.logging.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.middleware.backend.logging.model.MiddlewareApiCallLog;
import org.springframework.data.jpa.repository.Query;

public interface MiddlewareApiCallLogRepository
        extends JpaRepository<MiddlewareApiCallLog, Long>, JpaSpecificationExecutor<MiddlewareApiCallLog> {
    /** Lists all logs ordered by receivedAt descending. */
    List<MiddlewareApiCallLog> findAllByOrderByReceivedAtDesc();
    /** Checks whether any attempt exists for the given source transaction UUID. */
    boolean existsBySourceTransactionUUID(String sourceTransactionUUID);
    /** Returns the last attempt snapshot for the given source transaction UUID. */
    MiddlewareApiCallLog findTopBySourceTransactionUUIDOrderByAttemptNoDesc(String sourceTransactionUUID);
    /** Returns all attempts for the given source transaction UUID ordered ascending. */
    List<MiddlewareApiCallLog> findBySourceTransactionUUIDOrderByAttemptNoAsc(String sourceTransactionUUID);



    //** Statuses for the dashboard
    long countByReceivedAtBetween(LocalDateTime startOfDay, LocalDateTime endOfDay);

    long countByReceivedAtBetweenAndResponseCodeBetween(LocalDateTime startOfDay, LocalDateTime endOfDay, Integer res1, Integer res2);
    long countByReceivedAtBetweenAndStatus(LocalDateTime startOfDay, LocalDateTime endOfDay, String status);
    List<MiddlewareApiCallLog> findTop10ByResponseCodeBetweenOrderByReceivedAtDesc(Integer res1, Integer res2);
}
