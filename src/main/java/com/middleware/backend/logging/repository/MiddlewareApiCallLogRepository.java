package com.middleware.backend.logging.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.middleware.backend.logging.model.MiddlewareApiCallLog;

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
  }
