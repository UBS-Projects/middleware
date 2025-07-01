package com.middleware.backend.kaotocamel.controller;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.Map;
import java.util.HashMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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
import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;
import com.middleware.backend.kaotocamel.service.DynamicRouteService;
import com.middleware.backend.kaotocamel.service.RouteValidationService;
import com.middleware.backend.kaotocamel.spec.DynamicRouteSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@Slf4j
public class DynamicRouteController {

    private final DynamicRouteService routeService;
    private final RouteValidationService routeValidationService;

    @PostMapping("/deploy")
    public ResponseEntity<String> upload(@RequestBody String yaml, @RequestParam(required = false) String comment) {
        try {
            String result = routeService.uploadRoute(yaml, comment);
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
        try {
            RouteValidationResult result = routeValidationService.validateRoute(request.getYamlContent());
            return result.isValid() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("Route validation failed: {}", e.getMessage());
            RouteValidationResult errorResult = new RouteValidationResult();
            errorResult.setValid(false);
            errorResult.setErrorMessage("Validation failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
    }

    @PostMapping(value = "/test", consumes = "application/json", produces = "application/json")
    public ResponseEntity<RouteTestResult> testRoute(@RequestBody RouteTestRequest request) {
        try {
            RouteTestResult result = routeValidationService.testRoute(request.getYamlContent(), request.getTestMessage());
            return result.isSuccess() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("Route test failed: {}", e.getMessage());
            RouteTestResult errorResult = new RouteTestResult();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage("Test failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResult);
        }
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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        long startTime = System.currentTimeMillis();

        try {
            if (page < 0 || size <= 0 || size > 100) {
                return ResponseEntity.badRequest().build();
            }

            Pageable pageable = PageRequest.of(page, size);

            boolean hasFilters = Stream.of(routeId, description, path, httpMethod, comment, yamlContains)
                    .anyMatch(Objects::nonNull) ||
                    active != null || createdAfter != null || createdBefore != null;

            Page<DynamicRouteEntity> result;
            String queryType;

            if (hasFilters) {
                result = routeService.getLatestRoutesWithFiltersOptimized(
                        routeId, description, path, httpMethod, active,
                        comment, yamlContains, createdAfter, createdBefore, pageable);
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

            return ResponseEntity.ok().headers(headers).body(result);

        } catch (Exception e) {
            log.error("Error retrieving routes", e);

            try {
                Pageable pageable = PageRequest.of(page, size);
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

    @GetMapping("/all-versions")
    public ResponseEntity<Page<DynamicRouteEntity>> getAllVersionsWithFilters(
            @RequestParam(required = false) String routeId,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer version,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean defaultVersion,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String yamlContains,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

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
    public ResponseEntity<Page<DynamicRouteEntity>> getRouteVersions(
            @PathVariable String routeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

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
    public ResponseEntity<DynamicRouteEntity> getSpecificVersion(
            @PathVariable String routeId,
            @PathVariable int version) {

        try {
            return routeService.getSpecificVersion(routeId, version)
                    .map(route -> {
                        HttpHeaders headers = new HttpHeaders();
                        headers.add("X-Route-Id", routeId);
                        headers.add("X-Version", String.valueOf(version));
                        return ResponseEntity.ok().headers(headers).body(route);
                    })
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Error retrieving version {} for route: {}", version, routeId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/active")
    public ResponseEntity<Page<DynamicRouteEntity>> getActiveRoutes(
            @RequestParam(defaultValue = "0") int page,
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
}