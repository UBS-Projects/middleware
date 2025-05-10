package com.middleware.backend.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.middleware.backend.dto.WorkflowConfigDTO;
import com.middleware.backend.dto.WorkflowStepDTO;
import com.middleware.backend.mapper.WorkflowConfigMapper;
import com.middleware.backend.mapper.WorkflowStepMapper;
import com.middleware.backend.model.WorkflowConfig;
import com.middleware.backend.model.WorkflowStep;
import com.middleware.backend.repository.WorkflowConfigRepository;
import com.middleware.backend.repository.WorkflowStepRepository;
import com.middleware.backend.service.WorkflowService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowConfigRepository workflowConfigRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowConfigMapper workflowConfigMapper;
    private final WorkflowStepMapper workflowStepMapper;

    @Override
    @Transactional
    public WorkflowConfigDTO createWorkflow(WorkflowConfigDTO workflowConfigDTO, List<WorkflowStepDTO> steps) {
       
        WorkflowConfig workflow = workflowConfigMapper.toEntity(workflowConfigDTO);
         final WorkflowConfig savedworkflow =workflowConfigRepository.save(workflow);

        List<WorkflowStep> workflowSteps = steps.stream()
                .map(workflowStepMapper::toEntity)
                .peek(step -> step.setWorkflowConfig(savedworkflow))
                .collect(Collectors.toList());

        workflowStepRepository.saveAll(workflowSteps);

        return workflowConfigMapper.toDTO(workflow);
    }

    @Override
    @Transactional
    public WorkflowConfigDTO updateWorkflow(Long id, WorkflowConfigDTO workflowConfigDTO, List<WorkflowStepDTO> steps) {
        WorkflowConfig existingWorkflow = workflowConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));

        existingWorkflow.setName(workflowConfigDTO.getName());
        existingWorkflow.setDescription(workflowConfigDTO.getDescription());
        existingWorkflow.setActive(workflowConfigDTO.getActive());
        existingWorkflow.setUpdatedBy(workflowConfigDTO.getUpdatedBy());
        existingWorkflow.setUpdatedAt(workflowConfigDTO.getUpdatedAt());

        workflowConfigRepository.save(existingWorkflow);

        // Delete old steps and insert new
        workflowStepRepository.deleteAll(
            workflowStepRepository.findAll().stream()
                .filter(step -> step.getWorkflowConfig().getId().equals(id))
                .collect(Collectors.toList())
        );

        List<WorkflowStep> newSteps = steps.stream()
                .map(workflowStepMapper::toEntity)
                .peek(step -> step.setWorkflowConfig(existingWorkflow))
                .collect(Collectors.toList());

        workflowStepRepository.saveAll(newSteps);

        return workflowConfigMapper.toDTO(existingWorkflow);
    }

    @Override
    public WorkflowConfigDTO getWorkflow(Long id) {
        WorkflowConfig workflow = workflowConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));
        return workflowConfigMapper.toDTO(workflow);
    }

    @Override
    public void deleteWorkflow(Long id) {
        workflowStepRepository.deleteAll(
            workflowStepRepository.findAll().stream()
                .filter(step -> step.getWorkflowConfig().getId().equals(id))
                .collect(Collectors.toList())
        );
        workflowConfigRepository.deleteById(id);
    }
}
