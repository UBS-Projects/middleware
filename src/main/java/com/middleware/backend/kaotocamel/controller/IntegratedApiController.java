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
 * Enhanced Controller for managing IntegratedApi configurations with powerful filtering
 */
@RestController
@RequestMapping("/api/admin/integrated-apis")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Integrated APIs", description = "Manage DHIS2 API definitions with advanced filtering")
public class IntegratedApiController {

    private final IntegratedApiService service;

    @PostMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Create new integrated API definition")
    public ResponseEntity<IntegratedApiDto> create(@Valid @RequestBody IntegratedApiRequestDto request) {
        try {
            IntegratedApiDto result = service.create(request);
            log.info("Created integrated API: {}", result.getCode());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            log.error("Failed to create integrated API: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Update integrated API definition")
    public ResponseEntity<IntegratedApiDto> update(
            @PathVariable Long id,
            @Valid @RequestBody IntegratedApiRequestDto request) {
        try {
            IntegratedApiDto result = service.update(id, request);
            log.info("Updated integrated API: {}", result.getCode());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to update integrated API {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Soft delete integrated API")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            service.softDelete(id);
            log.info("Soft deleted integrated API: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete integrated API {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get integrated API by ID")
    public ResponseEntity<IntegratedApiDto> findById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get integrated API by code")
    public ResponseEntity<IntegratedApiDto> findByCode(@PathVariable String code) {
        return service.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "List integrated APIs with advanced filtering, pagination and sorting")
    public ResponseEntity<Page<IntegratedApiDto>> findAll(
            // Basic filters
            @Parameter(description = "Filter by code (partial match)")
            @RequestParam(required = false) String code,

            @Parameter(description = "Filter by name (partial match)")
            @RequestParam(required = false) String name,

            @Parameter(description = "Filter by API URL (partial match)")
            @RequestParam(required = false) String apiUrl,

            @Parameter(description = "Filter by API type (ANALYTICS, DATAVALUE, METADATA)")
            @RequestParam(required = false) String type,

            @Parameter(description = "Filter by integrated system (partial match)")
            @RequestParam(required = false) String integratedSystem,

            @Parameter(description = "Filter by active status")
            @RequestParam(required = false) Boolean isActive,

            @Parameter(description = "Filter by description (partial match)")
            @RequestParam(required = false) String description,

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

            // Pagination and sorting
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort criteria (format: field,direction). Example: name,asc or id,desc. Multiple sorts supported.")
            @RequestParam(defaultValue = "id,desc") String[] sort) {
         log.info("=== SORTING DEBUG ===");
        log.info("Received sort array: {}", Arrays.toString(sort));
        log.info("Sort array length: {}", sort != null ? sort.length : 0);

        if (sort != null) {
            for (int i = 0; i < sort.length; i++) {
                log.info("sort[{}] = '{}'", i, sort[i]);
            }
        }

        // Build dynamic sort
        Sort sortOrder = buildSortOrder(sort);
        log.info("Final Sort object: {}", sortOrder);
        log.info("=== END SORTING DEBUG ===");

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        log.info("Pageable: {}", pageable);

        // Use enhanced service method
        Page<IntegratedApiDto> result = service.findWithAdvancedFilters(
                code, name, apiUrl, type, integratedSystem, isActive, description,
                createdAfter, createdBefore, updatedAfter, updatedBefore,
                search, minId, maxId, pageable);

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(result.getTotalPages()))
                .header("X-Current-Page", String.valueOf(result.getNumber()))
                .header("X-Page-Size", String.valueOf(result.getSize()))
                .body(result);
    }

    @GetMapping("/types")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get available API types")
    public ResponseEntity<List<String>> getApiTypes() {
        return ResponseEntity.ok(Arrays.asList("ANALYTICS", "DATAVALUE", "METADATA"));
    }

    @GetMapping("/systems")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get distinct integrated systems")
    public ResponseEntity<List<String>> getIntegratedSystems() {
        List<String> systems = service.getDistinctIntegratedSystems();
        return ResponseEntity.ok(systems);
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Test integrated API connectivity")
    public ResponseEntity<Map<String, Object>> testApi(
            @PathVariable Long id,
            @RequestParam String pe) {
        try {
            Map<String, Object> result = service.testApiConnection(id, pe);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.ok(error);
        }
    }
    /**
     * Enhanced sort builder supporting multiple fields and directions
     */
    private Sort buildSortOrder(String[] sortParams) {
        log.debug("Received sort params: {}", Arrays.toString(sortParams));

        if (sortParams == null || sortParams.length == 0) {
            log.debug("No sort params provided, using default");
            return Sort.by(Sort.Order.asc("id"));
        }

        List<Sort.Order> orders = new ArrayList<>();

        // إذا كان المعامل منفصل (id, asc) بدل من (id,asc)
        if (sortParams.length == 2 && isValidSortField(sortParams[0])) {
            String field = sortParams[0].trim();
            String directionStr = sortParams[1].trim().toLowerCase();

            log.debug("Handling separated params - field: '{}', direction: '{}'", field, directionStr);

            Sort.Direction direction = "desc".equals(directionStr)
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;

            orders.add(new Sort.Order(direction, field));
            log.debug("Added separated sort order: {} {}", field, direction);

        } else {
            // الطريقة التقليدية للمعاملات المفصولة بفاصلة
            for (String sortParam : sortParams) {
                log.debug("Processing sort param: '{}'", sortParam);

                if (sortParam == null || sortParam.trim().isEmpty()) {
                    continue;
                }

                String[] parts = sortParam.split(",");
                log.debug("Split parts: {}", Arrays.toString(parts));

                if (parts.length >= 1) {
                    String field = parts[0].trim();
                    Sort.Direction direction = Sort.Direction.ASC; // Default

                    if (parts.length >= 2) {
                        String directionStr = parts[1].trim().toLowerCase();
                        log.debug("Direction string: '{}'", directionStr);

                        if ("desc".equals(directionStr)) {
                            direction = Sort.Direction.DESC;
                        }
                    }

                    if (isValidSortField(field)) {
                        Sort.Order order = new Sort.Order(direction, field);
                        orders.add(order);
                        log.debug("Added sort order: {} {}", field, direction);
                    } else {
                        log.warn("Invalid sort field: {}", field);
                    }
                }
            }
        }

        if (orders.isEmpty()) {
            log.debug("No valid orders found, using default");
            return Sort.by(Sort.Order.asc("id"));
        }

        Sort finalSort = Sort.by(orders);
        log.debug("Final sort object: {}", finalSort);
        return finalSort;
    }
    /**
     * Validate sortable field names
     */
    private boolean isValidSortField(String field) {
        Set<String> validFields = Set.of(
                "id", "code", "name", "apiUrl", "type",
                "integratedSystem", "isActive", "description",
                "createdAt", "updatedAt"
        );
        return validFields.contains(field);
    }
}