package com.middleware.backend.logging.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.middleware.backend.logging.model.MiddlewareApiCallLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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



    List<MiddlewareApiCallLog> findTop10ByResponseCodeBetweenOrderByReceivedAtDesc(Integer res1, Integer res2);

    @Query(value = """
    SELECT 
        DATE(received_at) AS date,
        COUNT(*) AS total,
        SUM(CASE WHEN response_code BETWEEN 200 AND 299 THEN 1 ELSE 0 END) AS succeeded,
        SUM(CASE WHEN response_code BETWEEN 300 AND 599 THEN 1 ELSE 0 END) AS failed,
        SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress
    FROM middleware_api_call_log
    WHERE received_at >= NOW() - INTERVAL '30 days'
    GROUP BY DATE(received_at)
    ORDER BY DATE(received_at)
    """, nativeQuery = true)
    List<Map<String, Object>> findTransactionGraphDataLast30Days();


    @Query(value = """
        SELECT
            (SELECT COUNT(*) FROM middleware_api_call_log WHERE received_at BETWEEN :start AND :end) AS all_transactions,
            (SELECT COUNT(*) FROM middleware_api_call_log WHERE received_at BETWEEN :start AND :end AND response_code BETWEEN 200 AND 299) AS succeeded_transactions,
            (SELECT COUNT(*) FROM middleware_api_call_log WHERE received_at BETWEEN :start AND :end AND response_code BETWEEN 400 AND 599) AS failed_transactions,
            (SELECT COUNT(*) FROM audit_logs WHERE start_time BETWEEN :start AND :end) AS user_events,
            (SELECT COUNT(*) FROM job_execution_logs WHERE start_time BETWEEN :start AND :end) AS all_jobs
        """, nativeQuery = true)
    Map<String, Object> getDashboardSummary(@Param("start") Timestamp start, @Param("end") Timestamp end);


}
