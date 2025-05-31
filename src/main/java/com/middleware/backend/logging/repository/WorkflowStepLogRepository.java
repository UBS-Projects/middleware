package com.middleware.backend.logging.repository;

import com.middleware.backend.logging.model.WorkflowStepLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WorkflowStepLogRepository extends JpaRepository<WorkflowStepLog, Long>, JpaSpecificationExecutor<WorkflowStepLog> {
}
