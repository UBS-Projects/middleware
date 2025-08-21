package com.middleware.backend.kaotocamel.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.middleware.backend.kaotocamel.spec.DynamicRouteLogsSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.backend.kaotocamel.dto.RouteTestRequest;
import com.middleware.backend.kaotocamel.dto.RouteTestResult;
import com.middleware.backend.kaotocamel.dto.RouteValidationResult;
import com.middleware.backend.kaotocamel.model.DynamicRouteAudit;
import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;
import com.middleware.backend.kaotocamel.service.DynamicRouteService;
import com.middleware.backend.kaotocamel.spec.DynamicRouteSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@Slf4j
public class DynamicRouteController {

    private final DynamicRouteService routeService;
    // private final RouteValidationService routeValidationService;

    @PostMapping("/create")
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
    public ResponseEntity<RouteValidationResult> validateRoute(@RequestBody RouteTestRequest request) {
        RouteValidationResult result = routeService.validateRoute(request.getYamlContent());
        return result.isValid() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @PostMapping(value = "/test", consumes = "application/json", produces = "application/json")
    public ResponseEntity<RouteTestResult> testRoute(@RequestBody RouteTestRequest request) {
        RouteTestResult result = routeService.testRoute(request.getYamlContent(), request.getTestMessage());
        return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/latest")
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

    @GetMapping("/all-versions")
    public ResponseEntity<Page<DynamicRouteEntity>> getAllVersionsWithFilters(
            @RequestParam(required = false) String routeId, @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer version, @RequestParam(required = false) String path,
            @RequestParam(required = false) String httpMethod, @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean defaultVersion, @RequestParam(required = false) String comment,
            @RequestParam(required = false) String yamlContains,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

        try {
            Specification<DynamicRouteEntity> spec = Specification
                    .where(DynamicRouteSpecification.hasField("routeId", routeId))
                    .and(DynamicRouteSpecification.hasField("description", description))
                    .and(DynamicRouteSpecification.hasField("version", version))
                    .and(DynamicRouteSpecification.hasField("path", path))
                    .and(DynamicRouteSpecification.hasField("httpMethod", httpMethod))
                    .and(DynamicRouteSpecification.hasField("active", active))
                    .and(DynamicRouteSpecification.hasField("defaultVersion", defaultVersion))
                    .and(DynamicRouteSpecification.containsComment(comment))
                    .and(DynamicRouteSpecification.containsInYaml(yamlContains))
                    .and(DynamicRouteSpecification.createdAfter(createdAfter))
                    .and(DynamicRouteSpecification.createdBefore(createdBefore));

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<DynamicRouteEntity> result = routeService.getAllRoutes(spec, pageable);

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Query-Type", "all-versions");
            headers.add("X-Total-Count", String.valueOf(result.getTotalElements()));

            return ResponseEntity.ok().headers(headers).body(result);

        } catch (Exception e) {
            log.error("Error in all-versions query", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{routeId}/versions")
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

    @GetMapping("/active")
    public ResponseEntity<Page<DynamicRouteEntity>> getActiveRoutes(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<DynamicRouteEntity> result = routeService.getActiveRoutes(pageable);

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Query-Type", "active-only");
            headers.add("X-Total-Count", String.valueOf(result.getTotalElements()));

            return ResponseEntity.ok().headers(headers).body(result);

        } catch (Exception e) {
            log.error("Error retrieving active routes", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            long totalRoutes = routeService.getAllRoutes(PageRequest.of(0, 1)).getTotalElements();

            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("totalRoutes", totalRoutes);

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            log.error("Health check failed", e);
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("timestamp", LocalDateTime.now());
            health.put("error", e.getMessage());

            return ResponseEntity.status(500).body(health);
        }
    }

// Add these methods to your DynamicRouteController

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
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
            @RequestParam(required = false) String sortDirection) {

        try {
            // Create sort object
            Sort sort = Sort.unsorted();
            if (sortBy != null && !sortBy.trim().isEmpty()) {
                Sort.Direction direction = Sort.Direction.ASC;
                if ("desc".equalsIgnoreCase(sortDirection)) {
                    direction = Sort.Direction.DESC;
                }
                String fieldName = mapColumnToField(sortBy);
                sort = Sort.by(direction, fieldName);
            } else {
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
            }

            // Use a large page size to get all results for export
            Pageable pageable = PageRequest.of(0, 10000, sort);

            boolean hasFilters = Stream.of(routeId, description, path, httpMethod, comment, yamlContains)
                    .anyMatch(Objects::nonNull) || active != null || createdAfter != null || createdBefore != null;

            Page<DynamicRouteEntity> result;
            if (hasFilters) {
                result = routeService.getLatestRoutesWithFiltersOptimized(routeId, description, path, httpMethod,
                        active, comment, yamlContains, createdAfter, createdBefore, pageable);
            } else {
                result = routeService.getLatestRoutesOptimized(pageable);
            }

            byte[] excelData = routeService.exportToExcel(result.getContent());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "routes_export_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".xlsx");
            headers.setContentLength(excelData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("Error exporting to Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportToCSV(
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
            @RequestParam(required = false) String sortDirection) {

        try {
            // Create sort object
            Sort sort = Sort.unsorted();
            if (sortBy != null && !sortBy.trim().isEmpty()) {
                Sort.Direction direction = Sort.Direction.ASC;
                if ("desc".equalsIgnoreCase(sortDirection)) {
                    direction = Sort.Direction.DESC;
                }
                String fieldName = mapColumnToField(sortBy);
                sort = Sort.by(direction, fieldName);
            } else {
                sort = Sort.by(Sort.Direction.DESC, "createdAt");
            }

            // Use a large page size to get all results for export
            Pageable pageable = PageRequest.of(0, 10000, sort);

            boolean hasFilters = Stream.of(routeId, description, path, httpMethod, comment, yamlContains)
                    .anyMatch(Objects::nonNull) || active != null || createdAfter != null || createdBefore != null;

            Page<DynamicRouteEntity> result;
            if (hasFilters) {
                result = routeService.getLatestRoutesWithFiltersOptimized(routeId, description, path, httpMethod,
                        active, comment, yamlContains, createdAfter, createdBefore, pageable);
            } else {
                result = routeService.getLatestRoutesOptimized(pageable);
            }

            byte[] csvData = routeService.exportToCSV(result.getContent());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "routes_export_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".csv");
            headers.setContentLength(csvData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvData);

        } catch (Exception e) {
            log.error("Error exporting to CSV", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/audits")
    public ResponseEntity<Page<?>> getRouteAudits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userName,
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
                    .where(DynamicRouteLogsSpecification.hasField("userName", userName, DynamicRouteLogsSpecification.MatchMode.CONTAINS))
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


}