package com.middleware.backend.camel.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.middleware.backend.camel.spec.DynamicRouteLogsSpecification;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.backend.camel.dto.RouteTestRequest;
import com.middleware.backend.camel.dto.RouteValidationResult;
import com.middleware.backend.camel.model.DynamicRouteAudit;
import com.middleware.backend.camel.model.DynamicRouteEntity;
import com.middleware.backend.camel.service.DynamicRouteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Slf4j
/**
 * REST controller exposing endpoints to create, update, manage, validate, query,
 * and export dynamic Camel routes and their audit logs.
 */
public class DynamicRouteController {

    private final DynamicRouteService routeService;
    // private final RouteValidationService routeValidationService;

    @PostMapping("/create")
    /**
     * Creates a new dynamic route from YAML content.
     * @param yaml Camel YAML content
     * @param comment optional audit comment
     * @return operation status text
     */
    @PreAuthorize("hasAnyAuthority('dynamicRoutes:create')")
    @Operation(
            summary = "Create a new dynamic route",
            description = "Deploys a new dynamic route using YAML content. Optional comment can be added. Requires 'dynamicRoutes:create' authority."
    )
    public ResponseEntity<String> create(@RequestBody String yaml, @RequestParam(required = false) String comment) {
        try {
            String result = routeService.updateRoute(yaml, comment, "create");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to deploy route: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to deploy route: " + e.getMessage());
        }
    }

    @PostMapping("/update")
    /**
     * Updates an existing dynamic route by uploading a new version.
     * @param yaml Camel YAML content
     * @param comment optional audit comment
     * @return operation status text
     */
    @PreAuthorize("hasAnyAuthority('dynamicRoutes:edit')")
    @Operation(
            summary = "Update an existing dynamic route",
            description = "Updates an existing dynamic route with provided YAML content. Optional comment can be added. Requires 'dynamicRoutes:edit' authority."
    )
    public ResponseEntity<String> update(@RequestBody String yaml, @RequestParam(required = false) String comment) {
        try {
            String result = routeService.updateRoute(yaml, comment, "update");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to deploy route: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Failed to deploy route: " + e.getMessage());
        }
    }

    @DeleteMapping("/{routeId}")
    /**
     * Deactivates a route by id and removes it from the Camel context.
     * @param routeId logical route identifier
     */
    @PreAuthorize("hasAnyAuthority('dynamicRoutes:delete')")
    @Operation(
            summary = "Deactivate a route",
            description = "Deactivates the route by its ID. Requires 'dynamicRoutes:delete' authority."
    )
    public ResponseEntity<String> deactivate(@PathVariable String routeId) {
        try {
            String result = routeService.deactivateRoute(routeId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to deactivate route {}: {}", routeId, e.getMessage());
            return ResponseEntity.badRequest().body("Failed to deactivate route: " + e.getMessage());
        }
    }

    @PostMapping("/{routeId}/revert/{version}")
    /**
     * Reverts a route to a previous version.
     */
    @PreAuthorize("hasAnyAuthority('dynamicRoutes:revert')")
    @Operation(
            summary = "Revert a route to a previous version",
            description = "Reverts a specific route to a specified version. Requires 'dynamicRoutes:revert' authority."
    )
    public ResponseEntity<String> revert(@PathVariable String routeId, @PathVariable int version) {
        try {
            String result = routeService.revertToVersion(routeId, version);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to revert route {} to version {}: {}", routeId, version, e.getMessage());
            return ResponseEntity.badRequest().body("Failed to revert route: " + e.getMessage());
        }
    }

    @PostMapping("/{routeId}/stop")
    /**
     * Stops a running route.
     */
    @PreAuthorize("hasAnyAuthority('dynamicRoutes:stop')")
    @Operation(
            summary = "Stop a route",
            description = "Stops a running route. Requires 'dynamicRoutes:stop' authority."
    )
    public ResponseEntity<String> stop(@PathVariable String routeId) {
        try {
            String result = routeService.stopRoute(routeId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to stop route {}: {}", routeId, e.getMessage());
            return ResponseEntity.badRequest().body("Failed to stop route: " + e.getMessage());
        }
    }

    @PostMapping("/{routeId}/start")
    /**
     * Starts the default version of a route.
     */
    @PreAuthorize("hasAnyAuthority('dynamicRoutes:start')")
    @Operation(
            summary = "Start a route",
            description = "Starts a stopped route. Requires 'dynamicRoutes:start' authority."
    )
    public ResponseEntity<String> start(@PathVariable String routeId) {
        try {
            String result = routeService.startRoute(routeId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to start route {}: {}", routeId, e.getMessage());
            return ResponseEntity.badRequest().body("Failed to start route: " + e.getMessage());
        }
    }

    @PostMapping(value = "/validate", consumes = "application/json", produces = "application/json")
    /**
     * Validates route YAML content by loading a temporary test route.
     */
    @PreAuthorize("hasAnyAuthority('dynamicRoutes:validate')")
    @Operation(
            summary = "Validate route YAML",
            description = "Validates route YAML content for syntax and structure. Requires 'dynamicRoutes:validate' authority."
    )
    public ResponseEntity<RouteValidationResult> validateRoute(@RequestBody RouteTestRequest request) {
        RouteValidationResult result = routeService.validateRoute(request.getYamlContent());
        return result.isValid() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/latest")
    /**
     * Returns the latest/default representation per route with optional filtering and sorting.
     */
    @PreAuthorize("hasAnyAuthority('dynamicRoutes:view')")
    @Operation(
            summary = "List latest routes",
            description = "Returns paginated list of latest dynamic routes with optional filters and sorting. Requires 'dynamicRoutes:view' authority."
    )
    public ResponseEntity<Page<DynamicRouteEntity>> getRoutes(
            @RequestParam(required = false) String routeId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String yamlContains,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        long startTime = System.currentTimeMillis();

        try {
            if (page < 0 || size <= 0 || size > 100) {
                return ResponseEntity.badRequest().build();
            }

            // Create sort object
            Sort sort = Sort.unsorted();
            if (sortBy != null && !sortBy.trim().isEmpty()) {
                Sort.Direction direction = Sort.Direction.ASC;
                if ("desc".equalsIgnoreCase(sortDirection)) {
                    direction = Sort.Direction.DESC;
                }

                // Map frontend column names to entity field names
                String fieldName = mapColumnToField(sortBy);
                sort = Sort.by(direction, fieldName);
            } else {
                // Default sort by createdAt descending if no sort specified
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
            }

            Pageable pageable = PageRequest.of(page, size, sort);

            boolean hasFilters = Stream.of(routeId, description, path, httpMethod, comment, yamlContains)
                    .anyMatch(Objects::nonNull) || active != null || createdAfter != null || createdBefore != null;

            Page<DynamicRouteEntity> result;
            String queryType;

            if (hasFilters) {
                result = routeService.getLatestRoutesWithFiltersOptimized(routeId, description, path, httpMethod,
                        active, comment, yamlContains, createdAfter, createdBefore, pageable);
                queryType = "filtered";
            } else {
                result = routeService.getLatestRoutesOptimized(pageable);
                queryType = "simple";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Total-Count", String.valueOf(result.getTotalElements()));
            headers.add("X-Total-Pages", String.valueOf(result.getTotalPages()));
            headers.add("X-Current-Page", String.valueOf(result.getNumber()));
            headers.add("X-Page-Size", String.valueOf(result.getSize()));
            headers.add("X-Query-Type", queryType);
            headers.add("X-Execution-Time", String.valueOf(System.currentTimeMillis() - startTime) + "ms");

            // Add sorting information to headers
            if (sortBy != null) {
                headers.add("X-Sort-By", sortBy);
                headers.add("X-Sort-Direction", sortDirection != null ? sortDirection : "asc");
            }

            return ResponseEntity.ok().headers(headers).body(result);

        } catch (Exception e) {
            log.error("Error retrieving routes", e);

            try {
                Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
                Page<DynamicRouteEntity> fallbackResult = routeService.getAllRoutes(pageable);

                HttpHeaders headers = new HttpHeaders();
                headers.add("X-Fallback-Mode", "true");
                headers.add("X-Total-Count", String.valueOf(fallbackResult.getTotalElements()));

                return ResponseEntity.ok().headers(headers).body(fallbackResult);

            } catch (Exception fallbackError) {
                log.error("Fallback query also failed", fallbackError);
                return ResponseEntity.internalServerError().build();
            }
        }
    }
    /**
     * Get list of all dynamic routes (routeId + path) for dropdowns
     * Same format as error-mappings/routes endpoint
     */
    @GetMapping("/routes")
    @PreAuthorize("hasAnyAuthority('dynamicRoutes:view')")
    @Operation(
            summary = "Get list of dynamic routes for dropdowns",
            description = "Returns list of all dynamic routes with routeId and path. Used for integration mapping and error configuration dropdowns."
    )
    public ResponseEntity<List<Map<String, String>>> getRoutesForDropdown() {
        try {
            // Get latest active routes
            List<DynamicRouteEntity> routes = routeService.getLatestActiveRoutesForDropdown();

            // Map to simple format: { routeId, path }
            List<Map<String, String>> routesList = routes.stream()
                    .map(route -> {
                        Map<String, String> routeMap = new HashMap<>();
                        routeMap.put("routeId", route.getRouteId());
                        routeMap.put("path", route.getPath());
                        return routeMap;
                    })
                    .collect(Collectors.toList());

            log.info("Returning {} routes for dropdown", routesList.size());
            return ResponseEntity.ok(routesList);

        } catch (Exception e) {
            log.error("Error fetching routes for dropdown", e);
            return ResponseEntity.internalServerError().build();
        }
    }
     private String mapColumnToField(String column) {
        switch (column.toLowerCase()) {
            case "routeid":
                return "routeId";
            case "version":
                return "version";
            case "active":
                return "active";
            case "comment":
                return "comment";
            case "createdat":
                return "createdAt";
            case "description":
                return "description";
            case "path":
                return "path";
            case "httpmethod":
                return "httpMethod";
            default:
                return "createdAt"; // Default fallback
        }
    }



    @GetMapping("/{routeId}/versions")
    @PreAuthorize("hasAnyAuthority('dynamicRoutes:view')")
    @Operation(
            summary = "Get route versions",
            description = "Returns paginated list of all versions of a specific route. Requires 'dynamicRoutes:view' authority."
    )
    public ResponseEntity<Page<DynamicRouteEntity>> getRouteVersions(@PathVariable String routeId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("version").descending());
            Page<DynamicRouteEntity> versions = routeService.getRoutesByRouteId(routeId, pageable);

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Route-Id", routeId);
            headers.add("X-Total-Versions", String.valueOf(versions.getTotalElements()));

            return ResponseEntity.ok().headers(headers).body(versions);

        } catch (Exception e) {
            log.error("Error retrieving versions for route: {}", routeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{routeId}/versions/{version}")
    /**
     * Retrieves a specific version document for the given route.
     */
    @PreAuthorize("hasAnyAuthority('dynamicRoutes:view')")
    @Operation(
            summary = "Get a specific route version",
            description = "Retrieves a specific version of a route by route ID and version number. Requires 'dynamicRoutes:view' authority."
    )
    public ResponseEntity<DynamicRouteEntity> getSpecificVersion(@PathVariable String routeId,
            @PathVariable int version) {

        try {
            return routeService.getSpecificVersion(routeId, version).map(route -> {
                HttpHeaders headers = new HttpHeaders();
                headers.add("X-Route-Id", routeId);
                headers.add("X-Version", String.valueOf(version));
                return ResponseEntity.ok().headers(headers).body(route);
            }).orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Error retrieving version {} for route: {}", version, routeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }




// Add these methods to your DynamicRouteController

    @GetMapping("/export/{type}")
    @PreAuthorize("hasAnyAuthority('dynamicRoutes:export')")
    @Operation(
            summary = "Export routes",
            description = "Exports dynamic routes to CSV or Excel. Supports filters and sorting. Requires 'dynamicRoutes:export' authority."
    )
    public ResponseEntity<byte[]> exportRoutes(
            @PathVariable("type") String type,
            @RequestParam(required = false) String routeId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String yamlContains,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(required = false, defaultValue = "updated_at") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        try {
            // Build filter map
            Map<String, String> filters = new HashMap<>();
            if (routeId != null) filters.put("routeId", routeId);
            if (description != null) filters.put("description", description);
            if (path != null) filters.put("path", path);
            if (httpMethod != null) filters.put("httpMethod", httpMethod);
            if (comment != null) filters.put("comment", comment);
            if (yamlContains != null) filters.put("yamlContains", yamlContains);
            if (active != null) filters.put("active", active.toString());
            if (createdAfter != null) filters.put("createdAfter", createdAfter.toString());
            if (createdBefore != null) filters.put("createdBefore", createdBefore.toString());
            if (sortBy != null) filters.put("sortBy", sortBy);
            if (sortDirection != null) filters.put("sortDirection", sortDirection);

            // Call service to export file
            byte[] fileBytes = routeService.exportFile(filters, type);

            // Prepare response headers
            String extension = type.equalsIgnoreCase("CSV") ? "csv" : "xlsx";
            String contentType = type.equalsIgnoreCase("CSV") ? "text/csv"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            String fileName = "routes_export_" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + "." + extension;

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);

        } catch (Exception e) {
            log.error("Error exporting routes", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/audits")
    /**
     * Retrieves paginated audit logs for route operations with filters.
     */
    @PreAuthorize("hasAnyAuthority('dynamicRoutesLogs:view')")
    @Operation(
            summary = "Get route audit logs",
            description = "Retrieves paginated list of route audit logs with filters. Requires 'dynamicRoutesLogs:view' authority."
    )
    public ResponseEntity<Page<?>> getRouteAudits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Integer version,
            @RequestParam(required = false) String routeId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
            ,@RequestParam(required = false, defaultValue = "timestamp") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
            ) {
        try {
            Specification<DynamicRouteAudit> spec = Specification
                    .where(DynamicRouteLogsSpecification.hasField("userEmail", userEmail, DynamicRouteLogsSpecification.MatchMode.CONTAINS))
                    .and(DynamicRouteLogsSpecification.hasField("action", action, DynamicRouteLogsSpecification.MatchMode.CONTAINS))
                    .and(DynamicRouteLogsSpecification.hasField("routeId", routeId, DynamicRouteLogsSpecification.MatchMode.CONTAINS))
                    .and(DynamicRouteLogsSpecification.createdBetween(startDate, endDate));
            if (version != null) {
                spec = spec.and(DynamicRouteLogsSpecification.hasField("version", String.valueOf(version), DynamicRouteLogsSpecification.MatchMode.EXACT));
            }
            Pageable pageable = PageRequest.of(page, size,
                    sortDirection.equalsIgnoreCase("asc")
                            ? Sort.by(sortedBy).ascending()
                            : Sort.by(sortedBy).descending());

            Page<?> result = routeService.getLatestRoutesLogs(spec, pageable);
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }



    @GetMapping("/audits/{id}")
    /**
     * Retrieves a single audit record by id.
     */
    @PreAuthorize("hasAnyAuthority('dynamicRoutesLogs:view')")
    @Operation(
            summary = "Get a specific route audit log",
            description = "Retrieves a specific audit log by ID. Requires 'dynamicRoutesLogs:view' authority."
    )
    public ResponseEntity<?> getRouteAudits(
        @PathVariable Long id
    ) {
        try {
            return ResponseEntity.ok().body(routeService.findById(id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }



    @PostMapping("/audits/{type}")
    /**
     * Exports audit logs to CSV or Excel based on the provided type.
     */
    @PreAuthorize("hasAnyAuthority('dynamicRoutesLogs:view')")
    @Operation(
            summary = "Export route audit logs",
            description = "Exports route audit logs to CSV or Excel. Requires 'dynamicRoutesLogs:view' authority."
    )
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Integer version,
            @RequestParam(required = false) String routeId,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "timestamp") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @PathVariable String type
    ) {
        try {
            Specification<DynamicRouteAudit> spec = Specification
                    .where(DynamicRouteLogsSpecification.hasField("userEmail", userEmail, DynamicRouteLogsSpecification.MatchMode.CONTAINS))
                    .and(DynamicRouteLogsSpecification.hasField("action", action, DynamicRouteLogsSpecification.MatchMode.EXACT))
                    .and(DynamicRouteLogsSpecification.hasField("routeId", routeId, DynamicRouteLogsSpecification.MatchMode.CONTAINS))
                    .and(DynamicRouteLogsSpecification.createdBetween(startDate, endDate));

            byte[] fileBytes = routeService.exportFile(spec, sortedBy, sortDirection, type);

            if (fileBytes == null || fileBytes.length == 0) {
                return ResponseEntity.noContent().build();
            }

            String fileName = "dynamic_routes_logs." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
            String contentType = type.equalsIgnoreCase("CSV")
                    ? "text/csv"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);

        } catch (Exception e) {
            log.error("Error exporting dynamic routes logs", e);
            return ResponseEntity.internalServerError().build();
        }
    }



}