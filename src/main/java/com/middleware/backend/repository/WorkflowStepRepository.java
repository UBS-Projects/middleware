package com.middleware.backend.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.middleware.backend.model.WorkflowStep;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {

    List<WorkflowStep> findByWorkflowConfigIdOrderByStepOrderAsc(Long workflowId);  


    
}