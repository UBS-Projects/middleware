package com.middleware.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.middleware.backend.model.WorkflowConfig;

@Repository
public interface WorkflowConfigRepository extends JpaRepository<WorkflowConfig, Long> {
}
