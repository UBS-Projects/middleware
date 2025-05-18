package com.middleware.backend.controller;

import com.middleware.backend.model.WorkflowConfig;
import com.middleware.backend.repository.WorkflowConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/workflow-configs")
@RequiredArgsConstructor
public class WorkflowConfigController {

    private final WorkflowConfigRepository repository;

    @PostMapping
    public WorkflowConfig create(@RequestBody WorkflowConfig workflowConfig) {
        return repository.save(workflowConfig);
    }

    @GetMapping("/{id}")
    public WorkflowConfig getById(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("WorkflowConfig not found"));
    }

    @GetMapping
    public List<WorkflowConfig> getAll() {
        return repository.findAll();
    }

    @PutMapping("/{id}")
    public WorkflowConfig update(@PathVariable Long id, @RequestBody WorkflowConfig updated) {
        WorkflowConfig existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkflowConfig not found"));
        updated.setId(existing.getId());
        return repository.save(updated);
    }

    @PatchMapping("/{id}")
    public WorkflowConfig partialUpdate(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        WorkflowConfig existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkflowConfig not found"));

        updates.forEach((key, value) -> {
            switch (key) {
            case "name" -> existing.setName((String) value);
            case "description" -> existing.setDescription((String) value);
            case "active" -> existing.setActive((Boolean) value);
            case "updatedBy" -> existing.setUpdatedBy(Long.valueOf(value.toString()));
            default -> log.warn("Unknown property '{}' in partial update, ignoring.", key);
            }
            
        });

        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
