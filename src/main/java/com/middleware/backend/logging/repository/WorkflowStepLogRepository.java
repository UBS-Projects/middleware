package com.middleware.backend.logging.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.middleware.backend.logging.model.WorkflowStepLog;

public interface WorkflowStepLogRepository
        extends JpaRepository<WorkflowStepLog, Long>, JpaSpecificationExecutor<WorkflowStepLog> {

    // Page<WorkflowStepLog> findAll(Specification<WorkflowStepLog> spec, Pageable
    // pageable);
}
