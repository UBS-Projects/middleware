package com.middleware.backend.kaotocamel.controller;

import com.middleware.backend.kaotocamel.dto.*;
import com.middleware.backend.kaotocamel.model.IntegrationMapping;
import com.middleware.backend.kaotocamel.service.*;
import com.middleware.backend.kaotocamel.spec.IntegrationMappingSpecification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Enhanced Controller for managing Integration Mappings linked to Dynamic Routes
 */
@RestController
@RequestMapping("/api/integration-mappings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Integration Mappings", description = "Manage field mappings for Dynamic Routes with advanced filtering")
public class IntegrationMappingAdminController {

    private final IntegrationMappingService service;

    @PostMapping
    @PreAuthorize("hasAuthority('integrationMapping:create')")
    @Operation(summary = "Create new integration mapping")
    public ResponseEntity<IntegrationMappingDto> create(
            @Valid @RequestBody IntegrationMappingRequestDto request) {
        try {
            IntegrationMappingDto result = service.create(request);
            log.info("Created integration mapping: {} -> {}",
                    request.getDynamicRouteId(), request.getExternalKey());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Failed to create integration mapping: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/import-excel")
    @PreAuthorize("hasAuthority('integrationMapping:import')")
    @Operation(summary = "Import mappings from Excel file")
    public ResponseEntity<Map<String, Object>> importFromExcel(
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Update existing mappings if found (default: true)")
            @RequestParam(required = false, defaultValue = "true") boolean updateExisting) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "File is empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "File must be Excel format (.xlsx or .xls)"));
        }

        try {
            Map<String, Object> result = service.importFromExcel(file.getInputStream(), updateExisting);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error importing from Excel", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('integrationMapping:edit')")
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('integrationMapping:view')")
    @Operation(summary = "Get integration mapping by ID")
    public ResponseEntity<IntegrationMappingDto> findById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('integrationMapping:view')")
    @Operation(summary = "List integration mappings with advanced filtering, pagination and sorting")
    public ResponseEntity<Page<IntegrationMappingDto>> findAll(
            // Basic filters
            @Parameter(description = "Filter by Dynamic Route ID (exact match)")
            @RequestParam(required = false) String dynamicRouteId,

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
            @Parameter(description = "Global search across multiple fields (partial match)")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by minimum ID")
            @RequestParam(required = false) Long minId,

            @Parameter(description = "Filter by maximum ID")
            @RequestParam(required = false) Long maxId,

            // Sorting parameters
            @Parameter(description = "Sort field")
            @RequestParam(required = false) String sortBy,

            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(required = false, defaultValue = "asc") String sortDir,

            // Pagination
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {

        // Build sort
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

        // Use enhanced service method with EXACT MATCH for dynamicRouteId
        Page<IntegrationMappingDto> result = service.findWithAdvancedFilters(
                dynamicRouteId,      // ← Exact match
                integratedApiId,
                integratedApiCode,   // ← Partial match
                mappingType,
                data,                // ← Partial match
                externalKey,         // ← Partial match
                attribute,           // ← Partial match
                isActive,
                notes,               // ← Partial match
                createdAfter,
                createdBefore,
                updatedAfter,
                updatedBefore,
                search,              // ← Global search (partial)
                minId,
                maxId,
                pageable);

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(result.getTotalPages()))
                .header("X-Current-Page", String.valueOf(result.getNumber()))
                .header("X-Page-Size", String.valueOf(result.getSize()))
                .body(result);
    }
    @GetMapping("/by-route/{routeId}")
    @PreAuthorize("hasAuthority('dynamicRoutes:view')")
    @Operation(summary = "Get all mappings for a Dynamic Route")
    public ResponseEntity<List<IntegrationMappingDto>> findByDynamicRoute(
            @PathVariable String routeId) {
        List<IntegrationMappingDto> result = service.findByDynamicRoute(routeId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/mapping-types")
    @PreAuthorize("hasAuthority('integrationMapping:view')")
    @Operation(summary = "Get available mapping types")
    public ResponseEntity<List<String>> getMappingTypes() {
        return ResponseEntity.ok(Arrays.asList(
                "DATA_ELEMENT",
                "DATA_ELEMENT_WITH_DISAGGREGATION",
                "INDICATOR"
        ));
    }

    @GetMapping("/dynamic-routes")
    @PreAuthorize("hasAuthority('dynamicRoutes:view')")
    @Operation(
            summary = "Get list of all configured Dynamic Route IDs",
            description = "Returns a distinct list of all Dynamic Route IDs that have active mappings"
    )
    public ResponseEntity<List<String>> getDynamicRoutes() {
        try {
            List<String> routes = service.getDistinctDynamicRoutes();
            log.info("Found {} distinct dynamic routes", routes.size());
            return ResponseEntity.ok(routes);
        } catch (Exception e) {
            log.error("Error fetching dynamic routes: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('integrationMapping:validate')")
    @Operation(summary = "Validate mapping configuration")
    public ResponseEntity<Map<String, Object>> validateMapping(
            @RequestBody IntegrationMappingRequestDto request) {
        Map<String, Object> validation = service.validateMapping(request);
        return ResponseEntity.ok(validation);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('integrationMapping:create')")
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
    @PreAuthorize("hasAuthority('integrationMapping:export')")
    @Operation(summary = "Export mappings configuration")
    public ResponseEntity<List<IntegrationMappingDto>> exportMappings(
            @RequestParam(required = false) String dynamicRouteId) {
        List<IntegrationMappingDto> mappings;
        if (dynamicRouteId != null) {
            mappings = service.findByDynamicRoute(dynamicRouteId);
        } else {
            mappings = service.findAll();
        }
        return ResponseEntity.ok(mappings);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('integrationMapping:import')")
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
     * Toggle active status of a mapping
     */
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasAuthority('integrationMapping:edit')")
    @Operation(
            summary = "Toggle mapping active status",
            description = "Toggles the active status of a mapping (active ↔ inactive)"
    )
    public ResponseEntity<IntegrationMappingDto> toggleStatus(@PathVariable Long id) {
        try {
            IntegrationMappingDto result = service.toggleActiveStatus(id);
            log.info("Toggled status for mapping {}: now {}", id,
                    result.getIsActive() ? "ACTIVE" : "INACTIVE");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to toggle status for mapping {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    /**
     * Exports integration mappings to CSV or Excel file with current filters applied.
     */
    @GetMapping("/export/{type}")
    @PreAuthorize("hasAuthority('integrationMapping:export')")
    @Operation(
            summary = "Export integration mappings",
            description = "Exports integration mappings to CSV or Excel format. Uses the same filters as the main listing API."
    )
    public ResponseEntity<byte[]> exportFile(
            @RequestParam(required = false) String dynamicRouteId,
            @RequestParam(required = false) Long integratedApiId,
            @RequestParam(required = false) String integratedApiCode,
            @RequestParam(required = false) String mappingType,
            @RequestParam(required = false) String data,
            @RequestParam(required = false) String externalKey,
            @RequestParam(required = false) String attribute,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedAfter,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedBefore,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long minId,
            @RequestParam(required = false) Long maxId,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @PathVariable("type") String type
    ) {
        try {
            // Build sort exactly like in findAll()
            Sort sortOrder = Sort.by("id").ascending();
            if (sortBy != null && !sortBy.trim().isEmpty() && isValidSortField(sortBy)) {
                if ("desc".equalsIgnoreCase(sortDir)) {
                    sortOrder = Sort.by(sortBy).descending();
                } else {
                    sortOrder = Sort.by(sortBy).ascending();
                }
            }

            // Use large page size to get all filtered results
            Pageable pageable = PageRequest.of(0, 100000, sortOrder);

            // Build specification with exact same filters as findAll()
            Specification<IntegrationMapping> spec = IntegrationMappingSpecification.buildSpecification(
                    dynamicRouteId, integratedApiId, integratedApiCode, mappingType,
                    data, externalKey, attribute, isActive, notes,
                    createdAfter, createdBefore, updatedAfter, updatedBefore,
                    search, minId, maxId
            );

            // Export the data
            byte[] fileBytes = service.exportFile(spec, pageable, type);

            // Set up response headers
            String fileName = "integration_mappings_export." +
                    (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");

            String contentType = type.equalsIgnoreCase("CSV")
                    ? "text/csv"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);

        } catch (Exception e) {
            log.error("Error exporting integration mappings to {}", type, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate sortable field names
     */
    private boolean isValidSortField(String field) {
        Set<String> validFields = Set.of(
                "id", "dynamicRouteId", "integratedApiId", "mappingType",
                "data", "attribute", "externalKey", "isActive", "notes",
                "createdAt", "updatedAt"
        );
        return validFields.contains(field);
    }
}