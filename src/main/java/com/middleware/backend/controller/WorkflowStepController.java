package com.middleware.backend.controller;

import com.middleware.backend.model.DestinationApi;
import com.middleware.backend.model.WorkflowConfig;
import com.middleware.backend.model.WorkflowStep;
import com.middleware.backend.repository.WorkflowStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/workflow-steps")
@RequiredArgsConstructor
public class WorkflowStepController {

    private final WorkflowStepRepository repository;

    @PostMapping
    public WorkflowStep create(@RequestBody WorkflowStep workflowStep) {
        return repository.save(workflowStep);
    }

    @PostMapping("/batch")
    public List<WorkflowStep> batchCreate(@RequestBody List<WorkflowStep> workflowSteps) {
        return repository.saveAll(workflowSteps);
    }

    @GetMapping("/{id}")
    public WorkflowStep getById(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("WorkflowStep not found"));
    }

    @GetMapping
    public List<WorkflowStep> getAll() {
        return repository.findAll();
    }

      @GetMapping("/by-config/{workflowConfigId}")
    public List<WorkflowStep> getByWorkflowConfigId(@PathVariable Long workflowConfigId) {
        return repository.findByWorkflowConfigIdOrderByStepOrderAsc(workflowConfigId);
    }
    

    @PutMapping("/{id}")
    public WorkflowStep update(@PathVariable Long id, @RequestBody WorkflowStep updated) {
        WorkflowStep existing = repository.findById(id).orElseThrow(() -> new RuntimeException("WorkflowStep not found"));
        updated.setId(existing.getId());
        return repository.save(updated);
    }

@PatchMapping("/{id}")
public WorkflowStep partialUpdate(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
    WorkflowStep existing = repository.findById(id)
        .orElseThrow(() -> new RuntimeException("WorkflowStep not found with id " + id));

    updates.forEach((key, value) -> {
        switch (key) {
            case "stepName" -> existing.setStepName((String) value);
            case "stepType" -> existing.setStepType((String) value);
            case "stepOrder" -> existing.setStepOrder((Integer) value);
            case "delaySeconds" -> existing.setDelaySeconds((Integer) value);
            case "conditionExpression" -> existing.setConditionExpression((String) value);
            case "retryCount" -> existing.setRetryCount((Integer) value);
            case "retryDelaySeconds" -> existing.setRetryDelaySeconds((Integer) value);
            case "updatedBy" -> existing.setUpdatedBy(Long.valueOf(value.toString()));


           case "workflowConfig" -> {
                if (value instanceof Map) {
                    Map<String, Object> workflowConfigMap = (Map<String, Object>) value;
                    if (workflowConfigMap.get("id") != null) {
                        WorkflowConfig workflowConfig = new WorkflowConfig();
                        workflowConfig.setId(Long.valueOf(workflowConfigMap.get("id").toString()));
                        existing.setWorkflowConfig(workflowConfig);
                    }else existing.setWorkflowConfig(null);
                }
            }

            case "destinationApi" -> {
                if (value instanceof Map) {
                    Map<String, Object> destinationApiMap = (Map<String, Object>) value;
                    if (destinationApiMap.get("id") != null) {
                        DestinationApi destinationApi = new DestinationApi();
                        destinationApi.setId(Long.valueOf(destinationApiMap.get("id").toString()));
                        existing.setDestinationApi(destinationApi);
                    }else existing.setDestinationApi(null);
                }
            }
            case "transformationExpression" -> existing.setTransformationExpression((Map<String, Object>) value);
            default -> log.warn("Unknown property '{}' in partial update for WorkflowStep, ignoring.", key);
        }
    });

    return repository.save(existing);
}



    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
