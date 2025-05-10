package com.middleware.backend.service;


import java.util.List;

import com.middleware.backend.dto.WorkflowConfigDTO;
import com.middleware.backend.dto.WorkflowStepDTO;

public interface WorkflowService {

    WorkflowConfigDTO createWorkflow(WorkflowConfigDTO workflowConfigDTO, List<WorkflowStepDTO> steps);

    WorkflowConfigDTO updateWorkflow(Long id, WorkflowConfigDTO workflowConfigDTO, List<WorkflowStepDTO> steps);

    WorkflowConfigDTO getWorkflow(Long id);

    void deleteWorkflow(Long id);
}
