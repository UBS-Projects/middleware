//package com.middleware.backend.kaotocamel.controller;
//
//import com.middleware.backend.kaotocamel.dto.IntegrationMappingDto;
//import com.middleware.backend.kaotocamel.service.IntegrationMappingService;
//import io.swagger.v3.oas.annotations.Operation;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import jakarta.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/integration-mappings")
//@RequiredArgsConstructor
//@Slf4j
//public class IntegrationMappingController {
//
//    private final IntegrationMappingService service;
//
//    @PostMapping
//    @PreAuthorize("hasAnyAuthority('integrationMapping:create')")
//    @Operation(
//            summary = "Create Integration Mapping",
//            description = "Creates a new integration mapping. Requires 'integrationMapping:create' authority."
//    )
//    public ResponseEntity<IntegrationMappingDto> create(@RequestBody IntegrationMappingDto dto) {
//        try {
//            IntegrationMappingDto result = service.create(dto);
//            return ResponseEntity.ok(result);
//        } catch (Exception e) {
//            log.error("Failed to create integration mapping: {}", e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    @PutMapping("/{id}")
//    @PreAuthorize("hasAnyAuthority('integrationMapping:edit')")
//    @Operation(
//            summary = "Update Integration Mapping",
//            description = "Updates an existing integration mapping by ID. Requires 'integrationMapping:edit' authority."
//    )
//    public ResponseEntity<IntegrationMappingDto> update(@PathVariable Long id, @RequestBody IntegrationMappingDto dto) {
//        try {
//            IntegrationMappingDto result = service.update(id, dto);
//            return ResponseEntity.ok(result);
//        } catch (RuntimeException e) {
//            log.error("Failed to update integration mapping {}: {}", id, e.getMessage());
//            return ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            log.error("Failed to update integration mapping {}: {}", id, e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    // UPDATED: Soft delete instead of hard delete
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAnyAuthority('integrationMapping:delete')")
//    @Operation(
//            summary = "Soft Delete Integration Mapping",
//            description = "Soft deletes an integration mapping by ID. Requires 'integrationMapping:delete' authority."
//    )
//    public ResponseEntity<IntegrationMappingDto> delete(@PathVariable Long id) {
//        try {
//            IntegrationMappingDto result = service.softDelete(id);
//            log.info("Successfully soft deleted integration mapping with ID: {}", id);
//            return ResponseEntity.ok(result);
//        } catch (RuntimeException e) {
//            log.error("Failed to soft delete integration mapping {}: {}", id, e.getMessage());
//            return ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            log.error("Failed to soft delete integration mapping {}: {}", id, e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    @GetMapping("/{id}")
//    @PreAuthorize("hasAnyAuthority('integrationMapping:view')")
//    @Operation(
//            summary = "Get Integration Mapping by ID",
//            description = "Retrieves an integration mapping by its ID. Requires 'integrationMapping:view' authority."
//    )
//    public ResponseEntity<IntegrationMappingDto> findById(@PathVariable Long id) {
//        return service.findById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    // UPDATED: Toggle status instead of setting specific value
//    @PatchMapping("/{id}/status")
//    @PreAuthorize("hasAnyAuthority('integrationMapping:edit')")
//    @Operation(
//            summary = "Toggle Integration Mapping Status",
//            description = "Toggles the active/inactive status of an integration mapping. Requires 'integrationMapping:edit' authority."
//    )
//    public ResponseEntity<IntegrationMappingDto> toggleStatus(@PathVariable Long id) {
//        try {
//            IntegrationMappingDto result = service.toggleStatus(id);
//            log.info("Successfully toggled status for integration mapping with ID: {}, new status: {}",
//                    id, result.getIsActive());
//            return ResponseEntity.ok(result);
//        } catch (RuntimeException e) {
//            log.error("Failed to toggle status for integration mapping {}: {}", id, e.getMessage());
//            return ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            log.error("Failed to toggle status for integration mapping {}: {}", id, e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    // OPTIONAL: Keep the old method for backward compatibility if needed
//    @PatchMapping("/{id}/status/set")
//    @PreAuthorize("hasAnyAuthority('integrationMapping:edit')")
//    @Operation(
//            summary = "Update Integration Mapping Status",
//            description = "Sets the active/inactive status of an integration mapping. Requires 'integrationMapping:edit' authority."
//    )
//    public ResponseEntity<IntegrationMappingDto> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
//        try {
//            IntegrationMappingDto result = service.updateStatus(id, request.isActive());
//            return ResponseEntity.ok(result);
//        } catch (RuntimeException e) {
//            log.error("Failed to update status for integration mapping {}: {}", id, e.getMessage());
//            return ResponseEntity.notFound().build();
//        } catch (Exception e) {
//            log.error("Failed to update status for integration mapping {}: {}", id, e.getMessage());
//            return ResponseEntity.badRequest().build();
//        }
//    }
//
//    @GetMapping
//    @PreAuthorize("hasAnyAuthority('integrationMapping:view')")
//    @Operation(
//            summary = "Get All Integration Mappings",
//            description = "Retrieves a paginated list of integration mappings with optional filters and sorting. Requires 'integrationMapping:view' authority."
//    )
//    public ResponseEntity<Page<IntegrationMappingDto>> findAll(
//            @RequestParam(required = false) String apiName,
//            @RequestParam(required = false) String externalSystem,
//            @RequestParam(required = false) String datasetId,
//            @RequestParam(required = false) String dataElementId,
//            @RequestParam(required = false) String externalKey,
//            @RequestParam(required = false) Boolean isActive,
//            @RequestParam(required = false) String notes,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
//            @RequestParam(required = false) String sortBy,
//            @RequestParam(required = false) String sortDirection,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//
//        long startTime = System.currentTimeMillis();
//
//        try {
//            if (page < 0 || size <= 0 || size > 100) {
//                return ResponseEntity.badRequest().build();
//            }
//
//            // Create sort object
//            Sort sort = createSortObject(sortBy, sortDirection);
//            Pageable pageable = PageRequest.of(page, size, sort);
//
//            Page<IntegrationMappingDto> result;
//            boolean hasFilters = hasAnyFilter(apiName, externalSystem, datasetId, dataElementId,
//                    externalKey, isActive, createdAfter, createdBefore, notes);
//
//            log.info("Search request - hasFilters: {}, apiName: {}, externalSystem: {}, isActive: {}",
//                    hasFilters, apiName, externalSystem, isActive);
//
//            if (hasFilters) {
//                result = service.findWithFilters(apiName, externalSystem, datasetId, dataElementId,
//                        externalKey, isActive, createdAfter, createdBefore, notes, pageable);
//            } else {
//                result = service.findAll(pageable);
//            }
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.add("X-Total-Count", String.valueOf(result.getTotalElements()));
//            headers.add("X-Total-Pages", String.valueOf(result.getTotalPages()));
//            headers.add("X-Current-Page", String.valueOf(result.getNumber()));
//            headers.add("X-Page-Size", String.valueOf(result.getSize()));
//            headers.add("X-Has-Filters", String.valueOf(hasFilters));
//            headers.add("X-Execution-Time", String.valueOf(System.currentTimeMillis() - startTime) + "ms");
//
//            if (sortBy != null) {
//                headers.add("X-Sort-By", sortBy);
//                headers.add("X-Sort-Direction", sortDirection != null ? sortDirection : "asc");
//            }
//
//            return ResponseEntity.ok().headers(headers).body(result);
//
//        } catch (Exception e) {
//            log.error("Error retrieving integration mappings", e);
//            return ResponseEntity.internalServerError().build();
//        }
//    }
//
//    // ===== EXPORT ENDPOINTS =====
//
//    @GetMapping("/export/excel")
//    @PreAuthorize("hasAnyAuthority('integrationMapping:export')")
//    @Operation(
//            summary = "Export Integration Mappings to Excel",
//            description = "Exports integration mappings to an Excel file, optionally applying filters. Requires 'integrationMapping:export' authority."
//    )
//    public void exportToExcel(
//            @RequestParam(required = false) String apiName,
//            @RequestParam(required = false) String externalSystem,
//            @RequestParam(required = false) String datasetId,
//            @RequestParam(required = false) String dataElementId,
//            @RequestParam(required = false) String externalKey,
//            @RequestParam(required = false) Boolean isActive,
//            @RequestParam(required = false) String notes,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
//            @RequestParam(required = false) String sortBy,
//            @RequestParam(required = false) String sortDirection,
//            HttpServletResponse response) throws IOException {
//
//        log.info("=== EXCEL EXPORT START ===");
//        log.info("Export request - apiName: {}, externalSystem: {}, isActive: {}, notes: {}",
//                apiName, externalSystem, isActive, notes != null ? "provided" : "null");
//
//        try {
//            // Check if filters are applied
//            boolean hasFilters = hasAnyFilter(apiName, externalSystem, datasetId, dataElementId,
//                    externalKey, isActive, createdAfter, createdBefore, notes);
//
//            log.info("Has filters: {}", hasFilters);
//
//            // Create sort object
//            Sort sort = createSortObject(sortBy, sortDirection);
//
//            // Get FILTERED data directly as List - NOT Page
//            List<IntegrationMappingDto> data;
//
//            if (hasFilters) {
//                log.info("Applying filters for Excel export");
//                data = service.findAllWithFiltersForExport(apiName, externalSystem, datasetId, dataElementId,
//                        externalKey, isActive, createdAfter, createdBefore, notes, sort);
//            } else {
//                log.info("No filters applied - exporting all data");
//                data = service.findAllForExport(sort);
//            }
//
//            log.info("Found {} records for export", data.size());
//
//            // Set response headers for Excel download
//            setExcelResponseHeaders(response, hasFilters);
//
//            // Generate Excel file WITHOUT filter info in the file content
//            service.exportToExcel(data, response.getOutputStream());
//
//            log.info("Successfully exported {} integration mappings to Excel (filtered: {})",
//                    data.size(), hasFilters);
//
//        } catch (Exception e) {
//            log.error("Error exporting integration mappings to Excel", e);
//            handleExportError(response, "Excel", e);
//        }
//
//        log.info("=== EXCEL EXPORT END ===");
//    }
//
//    @GetMapping("/export/csv")
//    @PreAuthorize("hasAnyAuthority('integrationMapping:export')")
//    @Operation(
//            summary = "Export Integration Mappings to CSV",
//            description = "Exports integration mappings to a CSV file, optionally applying filters. Requires 'integrationMapping:export' authority."
//    )
//    public void exportToCSV(
//            @RequestParam(required = false) String apiName,
//            @RequestParam(required = false) String externalSystem,
//            @RequestParam(required = false) String datasetId,
//            @RequestParam(required = false) String dataElementId,
//            @RequestParam(required = false) String externalKey,
//            @RequestParam(required = false) Boolean isActive,
//            @RequestParam(required = false) String notes,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
//            @RequestParam(required = false) String sortBy,
//            @RequestParam(required = false) String sortDirection,
//            HttpServletResponse response) throws IOException {
//
//        log.info("=== CSV EXPORT START ===");
//        log.info("Export request - apiName: {}, externalSystem: {}, isActive: {}, notes: {}",
//                apiName, externalSystem, isActive, notes != null ? "provided" : "null");
//
//        try {
//            // Check if filters are applied
//            boolean hasFilters = hasAnyFilter(apiName, externalSystem, datasetId, dataElementId,
//                    externalKey, isActive, createdAfter, createdBefore, notes);
//
//            log.info("Has filters: {}", hasFilters);
//
//            // Create sort object
//            Sort sort = createSortObject(sortBy, sortDirection);
//
//            // Get FILTERED data directly as List - NOT Page
//            List<IntegrationMappingDto> data;
//
//            if (hasFilters) {
//                log.info("Applying filters for CSV export");
//                data = service.findAllWithFiltersForExport(apiName, externalSystem, datasetId, dataElementId,
//                        externalKey, isActive, createdAfter, createdBefore, notes, sort);
//            } else {
//                log.info("No filters applied - exporting all data");
//                data = service.findAllForExport(sort);
//            }
//
//            log.info("Found {} records for export", data.size());
//
//            // Set response headers for CSV download
//            setCSVResponseHeaders(response, hasFilters);
//
//            // Generate CSV file WITHOUT filter info in the file content
//            service.exportToCSV(data, response.getWriter());
//
//            log.info("Successfully exported {} integration mappings to CSV (filtered: {})",
//                    data.size(), hasFilters);
//
//        } catch (Exception e) {
//            log.error("Error exporting integration mappings to CSV", e);
//            handleExportError(response, "CSV", e);
//        }
//
//        log.info("=== CSV EXPORT END ===");
//    }
//
//    // ===== HELPER METHODS =====
//
//    private Sort createSortObject(String sortBy, String sortDirection) {
//        if (sortBy != null && !sortBy.trim().isEmpty()) {
//            Sort.Direction direction = Sort.Direction.ASC;
//            if ("desc".equalsIgnoreCase(sortDirection)) {
//                direction = Sort.Direction.DESC;
//            }
//            String fieldName = mapColumnToField(sortBy);
//            return Sort.by(direction, fieldName);
//        } else {
//            return Sort.by(Sort.Direction.DESC, "createdAt");
//        }
//    }
//
//    private void setExcelResponseHeaders(HttpServletResponse response, boolean hasFilters) {
//        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
//        String filterSuffix = hasFilters ? "_filtered" : "_all";
//        String filename = "integration_mappings" + filterSuffix + "_" + timestamp + ".xlsx";
//
//        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
//        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
//        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
//        response.setHeader("Pragma", "no-cache");
//        response.setHeader("Expires", "0");
//    }
//
//    private void setCSVResponseHeaders(HttpServletResponse response, boolean hasFilters) {
//        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
//        String filterSuffix = hasFilters ? "_filtered" : "_all";
//        String filename = "integration_mappings" + filterSuffix + "_" + timestamp + ".csv";
//
//        response.setContentType("text/csv");
//        response.setCharacterEncoding("UTF-8");
//        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
//        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
//        response.setHeader("Pragma", "no-cache");
//        response.setHeader("Expires", "0");
//    }
//
//    private void handleExportError(HttpServletResponse response, String format, Exception e) throws IOException {
//        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//        response.setContentType("text/plain");
//        response.getWriter().write("Error generating " + format + " export: " + e.getMessage());
//    }
//
//    private String mapColumnToField(String column) {
//        if (column == null) return "createdAt";
//
//        switch (column.toLowerCase()) {
//            case "id": return "id";
//            case "apiname": return "apiName";
//            case "externalsystem": return "externalSystem";
//            case "datasetid": return "datasetId";
//            case "dataelementid": return "dataElementId";
//            case "categoryoptioncomoid": return "categoryOptionComboId";
//            case "attributeoptioncomoid": return "attributeOptionComboId";
//            case "externalkey": return "externalKey";
//            case "isactive": return "isActive";
//            case "createdat": return "createdAt";
//            case "notes": return "notes";
//            default: return "createdAt";
//        }
//    }
//
//    private boolean hasAnyFilter(String apiName, String externalSystem, String datasetId,
//                                 String dataElementId, String externalKey, Boolean isActive,
//                                 LocalDateTime createdAfter, LocalDateTime createdBefore, String notes) {
//        boolean result = (apiName != null && !apiName.trim().isEmpty()) ||
//                (externalSystem != null && !externalSystem.trim().isEmpty()) ||
//                (datasetId != null && !datasetId.trim().isEmpty()) ||
//                (dataElementId != null && !dataElementId.trim().isEmpty()) ||
//                (externalKey != null && !externalKey.trim().isEmpty()) ||
//                (notes != null && !notes.trim().isEmpty()) ||
//                isActive != null ||
//                createdAfter != null ||
//                createdBefore != null;
//
//        log.debug("hasAnyFilter result: {}", result);
//        return result;
//    }
//
//    // ===== DTOs =====
//
//    public static class StatusUpdateRequest {
//        private boolean isActive;
//
//        public boolean isActive() {
//            return isActive;
//        }
//
//        public void setActive(boolean active) {
//            isActive = active;
//        }
//    }
//}