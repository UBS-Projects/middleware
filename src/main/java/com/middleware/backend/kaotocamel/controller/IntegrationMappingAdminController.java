package com.middleware.backend.kaotocamel.controller;

import com.middleware.backend.kaotocamel.dto.*;
import com.middleware.backend.kaotocamel.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Enhanced Controller for managing Integration Mappings with advanced filtering and sorting
 */
@RestController
@RequestMapping("/api/admin/integration-mappings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Integration Mappings", description = "Manage field mappings for middleware APIs with advanced filtering")
public class IntegrationMappingAdminController {

    private final IntegrationMappingService service;

    @PostMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Create new integration mapping")
    public ResponseEntity<IntegrationMappingDto> create(
            @Valid @RequestBody IntegrationMappingRequestDto request) {
        try {
            IntegrationMappingDto result = service.create(request);
            log.info("Created integration mapping: {} -> {}",
                    request.getMiddlewareApiName(), request.getExternalKey());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Failed to create integration mapping: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Update integration mapping")
    public ResponseEntity<IntegrationMappingDto> update(
            @PathVariable Long id,
            @Valid @RequestBody IntegrationMappingRequestDto request) {
        try {
            IntegrationMappingDto result = service.update(id, request);
            log.info("Updated integration mapping: {}", id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to update integration mapping {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Soft delete integration mapping")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.softDelete(id);
            log.info("Soft deleted integration mapping: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete integration mapping {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get integration mapping by ID")
    public ResponseEntity<IntegrationMappingDto> findById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "List integration mappings with advanced filtering, pagination and sorting")
    public ResponseEntity<Page<IntegrationMappingDto>> findAll(
            // Basic filters
            @Parameter(description = "Filter by middleware API name (partial match)")
            @RequestParam(required = false) String middlewareApiName,

            @Parameter(description = "Filter by integrated API ID")
            @RequestParam(required = false) Long integratedApiId,

            @Parameter(description = "Filter by integrated API code (partial match)")
            @RequestParam(required = false) String integratedApiCode,

            @Parameter(description = "Filter by mapping type")
            @RequestParam(required = false) String mappingType,

            @Parameter(description = "Filter by data (partial match)")
            @RequestParam(required = false) String data,

            @Parameter(description = "Filter by external key (partial match)")
            @RequestParam(required = false) String externalKey,

            @Parameter(description = "Filter by attribute (partial match)")
            @RequestParam(required = false) String attribute,

            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean isActive,

            @Parameter(description = "Filter by notes (partial match)")
            @RequestParam(required = false) String notes,

            // Date filters
            @Parameter(description = "Filter by created after date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,

            @Parameter(description = "Filter by created before date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,

            @Parameter(description = "Filter by updated after date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedAfter,

            @Parameter(description = "Filter by updated before date (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedBefore,

            // Advanced filters
            @Parameter(description = "Global search across multiple fields")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by minimum ID")
            @RequestParam(required = false) Long minId,

            @Parameter(description = "Filter by maximum ID")
            @RequestParam(required = false) Long maxId,

            // Sorting parameters (separate approach like we solved for IntegratedApi)
            @Parameter(description = "Sort field")
            @RequestParam(required = false) String sortBy,

            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(required = false, defaultValue = "asc") String sortDir,

            // Pagination
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {

        // Build sort simply (avoiding the array parsing issue)
        Sort sortOrder = Sort.by("id").ascending(); // default

        if (sortBy != null && !sortBy.trim().isEmpty() && isValidSortField(sortBy)) {
            if ("desc".equalsIgnoreCase(sortDir)) {
                sortOrder = Sort.by(sortBy).descending();
                log.info("Applied descending sort on: {}", sortBy);
            } else {
                sortOrder = Sort.by(sortBy).ascending();
                log.info("Applied ascending sort on: {}", sortBy);
            }
        }

        log.info("Final sort: {}", sortOrder);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        // Use enhanced service method
        Page<IntegrationMappingDto> result = service.findWithAdvancedFilters(
                middlewareApiName, integratedApiId, integratedApiCode, mappingType,
                data, externalKey, attribute, isActive, notes,
                createdAfter, createdBefore, updatedAfter, updatedBefore,
                search, minId, maxId, pageable);

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(result.getTotalPages()))
                .header("X-Current-Page", String.valueOf(result.getNumber()))
                .header("X-Page-Size", String.valueOf(result.getSize()))
                .body(result);
    }

    @GetMapping("/by-middleware/{apiName}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get all mappings for a middleware API")
    public ResponseEntity<List<IntegrationMappingDto>> findByMiddlewareApi(
            @PathVariable String apiName) {
        List<IntegrationMappingDto> result = service.findByMiddlewareApi("/" + apiName);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/mapping-types")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get available mapping types")
    public ResponseEntity<List<String>> getMappingTypes() {
        return ResponseEntity.ok(Arrays.asList(
                "DATA_ELEMENT",
                "DATA_ELEMENT_WITH_DISAGGREGATION",
                "DATA_ELEMENT_WITH_DISAGGREGATION_AND_ATTRIBUTE",
                "INDICATOR"
        ));
    }

    @GetMapping("/middleware-apis")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get list of configured middleware APIs")
    public ResponseEntity<List<String>> getMiddlewareApis() {
        List<String> apis = service.getDistinctMiddlewareApis();
        return ResponseEntity.ok(apis);
    }

    @PostMapping("/validate")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Validate mapping configuration")
    public ResponseEntity<Map<String, Object>> validateMapping(
            @RequestBody IntegrationMappingRequestDto request) {
        Map<String, Object> validation = service.validateMapping(request);
        return ResponseEntity.ok(validation);
    }

    @PostMapping("/batch")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Create multiple mappings in batch")
    public ResponseEntity<Map<String, Object>> createBatch(
            @RequestBody List<IntegrationMappingRequestDto> requests) {
        try {
            List<IntegrationMappingDto> created = service.createBatch(requests);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("created", created.size());
            result.put("mappings", created);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/export")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Export mappings configuration")
    public ResponseEntity<List<IntegrationMappingDto>> exportMappings(
            @RequestParam(required = false) String middlewareApiName) {
        List<IntegrationMappingDto> mappings;
        if (middlewareApiName != null) {
            mappings = service.findByMiddlewareApi(middlewareApiName);
        } else {
            mappings = service.findAll();
        }
        return ResponseEntity.ok(mappings);
    }

    @PostMapping("/import")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Import mappings configuration")
    public ResponseEntity<Map<String, Object>> importMappings(
            @RequestBody List<IntegrationMappingRequestDto> mappings) {
        try {
            Map<String, Object> result = service.importMappings(mappings);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Validate sortable field names
     */
    private boolean isValidSortField(String field) {
        Set<String> validFields = Set.of(
                "id", "middlewareApiName", "integratedApiId", "mappingType",
                "data", "attribute", "externalKey", "isActive", "notes",
                "createdAt", "updatedAt"
        );
        return validFields.contains(field);
    }
}