package com.middleware.backend.logging.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.middleware.backend.logging.model.MiddlewareApiCallLog;

public interface MiddlewareApiCallLogRepository
                extends JpaRepository<MiddlewareApiCallLog, Long>, JpaSpecificationExecutor<MiddlewareApiCallLog> {
        List<MiddlewareApiCallLog> findAllByOrderByReceivedAtDesc();
        // Page<MiddlewareApiCallLog> findAll(Specification<MiddlewareApiCallLog> spec,
        // Pageable pageable);

}
