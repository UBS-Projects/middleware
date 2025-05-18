package com.middleware.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.backend.model.ApiEndpoint;
import com.middleware.backend.model.WorkflowConfig;
import com.middleware.backend.repository.ApiEndpointRepository;
import com.middleware.backend.util.JsonStringToJsonObject;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/endpoints")
@RequiredArgsConstructor
public class ApiEndpointController {

    private final ApiEndpointRepository repository;

    @PostMapping
    public ApiEndpoint create(@RequestBody ApiEndpoint apiEndpoint) {
        return repository.save(apiEndpoint);
    }

    @GetMapping("/{id}")
    public ApiEndpoint getById(@PathVariable Long id) throws Exception {
        return  repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ApiEndpoint not found"));
        
    }

    @GetMapping
    public List<ApiEndpoint> getAll() {
        return repository.findAll();
    }

    @PutMapping("/{id}")
    public ApiEndpoint update(@PathVariable Long id, @RequestBody ApiEndpoint updated) {
        ApiEndpoint existing = repository.findById(id).orElseThrow(() -> new RuntimeException("ApiEndpoint not found"));
        updated.setId(existing.getId());
        return repository.save(updated);
    }

    /*
     * @PutMapping("/{id}") public ApiEndpoint update(@PathVariable Long
     * id, @RequestBody ApiEndpoint updated) { ApiEndpoint existing =
     * repository.findById(id) .orElseThrow(() -> new
     * RuntimeException("ApiEndpoint not found"));
     * 
     * // Only update fields if they are provided (non-null) in the request if
     * (updated.getEndpointPath() != null) {
     * existing.setEndpointPath(updated.getEndpointPath()); }
     * 
     * if (updated.getMethod() != null) { existing.setMethod(updated.getMethod()); }
     * 
     * if (updated.getBaseUrl() != null) {
     * existing.setBaseUrl(updated.getBaseUrl()); }
     * 
     * if (updated.getInputTemplate() != null) {
     * existing.setInputTemplate(updated.getInputTemplate()); }
     * 
     * if (updated.getOutputTemplate() != null) {
     * existing.setOutputTemplate(updated.getOutputTemplate()); }
     * 
     * if (updated.getHeaders() != null) {
     * existing.setHeaders(updated.getHeaders()); }
     * 
     * if (updated.getTriggerWorkflow() != null) {
     * existing.setTriggerWorkflow(updated.getTriggerWorkflow()); }
     * 
     * 
     * 
     * return repository.save(existing); }
     */

    @PatchMapping("/{id}")
    public ApiEndpoint partialUpdate(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        ApiEndpoint existing = repository.findById(id).orElseThrow(() -> new RuntimeException("ApiEndpoint not found"));

        updates.forEach((key, value) -> {
            switch (key) {
            case "name" -> existing.setName((String) value);
            case "endpointPath" -> existing.setEndpointPath((String) value);
            case "method" -> existing.setMethod((String) value);
            case "authenticationType" -> existing.setAuthenticationType((String) value);
            case "baseUrl" -> existing.setBaseUrl((String) value);
            case "inputTemplate" -> existing.setInputTemplate( (Map<String,Object>)value);
             case "inputheader" -> existing.setInputheader( (Map<String,Object>)value);
            case "outputTemplate" -> existing.setOutputTemplate((Map<String,Object>) value);
            case "outputheaders" -> {
                if (value instanceof Map) {
                    existing.setOutputheaders((Map<String, Object>) value);
                }
            }
            case "queryParams" -> {
                if (value instanceof Map) {
                    existing.setQueryParams((Map<String, Object>) value);
                }
            }
            case "status" -> existing.setStatus((String) value);
            case "description" -> existing.setDescription((String) value);
            case "triggerWorkflow" -> {
                if (value instanceof Map) {
                    Map<String, Object> workflowConfigMap = (Map<String, Object>) value;
                    if (workflowConfigMap.get("id") != null) {
                        WorkflowConfig workflowConfig = new WorkflowConfig();
                        workflowConfig.setId(Long.valueOf(workflowConfigMap.get("id").toString()));
                        existing.setTriggerWorkflow(workflowConfig);
                    } else
                        existing.setTriggerWorkflow(null);
                }
            }
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
