package com.middleware.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.middleware.backend.model.WorkflowStep;
import com.middleware.backend.repository.WorkflowStepRepository;

import lombok.RequiredArgsConstructor;



@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {
    private final WorkflowService WorkflowService;
    private final WorkflowStepRepository workflowStepRepository;

    public List<WorkflowStep> getSteps(Long workflowId) {
        return workflowStepRepository.findByWorkflowConfigIdOrderByStepOrderAsc(workflowId);
    
        
        
    }
}