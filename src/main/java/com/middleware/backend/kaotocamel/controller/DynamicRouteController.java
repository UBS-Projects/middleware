package com.middleware.backend.kaotocamel.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
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
import com.middleware.backend.kaotocamel.spec.DynamicRouteSpecification;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class DynamicRouteController {

    private final DynamicRouteService routeService;
    // private final RouteValidationService routeValidationService;

    @PostMapping("/deploy")
    public String upload(@RequestBody String yaml, @RequestParam(required = false) String comment) {
        return routeService.uploadRoute(yaml, comment);
    }

    @DeleteMapping("/{routeId}")
    public String deactivate(@PathVariable String routeId) {
        return routeService.deactivateRoute(routeId);
    }

    @PostMapping("/{routeId}/revert/{version}")
    public String revert(@PathVariable String routeId, @PathVariable int version) {
        return routeService.revertToVersion(routeId, version);
    }
    /*
     * @GetMapping public Page<DynamicRouteEntity>
     * getAllRoutes(@RequestParam(defaultValue = "0") int page,
     * 
     * @RequestParam(defaultValue = "10") int size) { return
     * routeService.getAllRoutes(PageRequest.of(page, size)); }
     * 
     * @GetMapping("/active") public Page<DynamicRouteEntity>
     * getActiveRoutes(@RequestParam(defaultValue = "0") int page,
     * 
     * @RequestParam(defaultValue = "10") int size) { return
     * routeService.getActiveRoutes(PageRequest.of(page, size)); }
     * 
     * @GetMapping("/{routeId}") public Page<DynamicRouteEntity>
     * getRoutesByRouteId(@PathVariable String routeId,
     * 
     * @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue =
     * "10") int size) { return routeService.getRoutesByRouteId(routeId,
     * PageRequest.of(page, size)); }
     * 
     * @GetMapping("/{routeId}/versions") public List<DynamicRouteEntity>
     * versions(@PathVariable String routeId) { return
     * routeService.listVersions(routeId); }
     */

    @PostMapping("/{routeId}/stop")
    public String stop(@PathVariable String routeId) {
        return routeService.stopRoute(routeId);
    }

    @PostMapping("/{routeId}/start")
    public String start(@PathVariable String routeId) {
        return routeService.startRoute(routeId);
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

    @GetMapping
    public Page<DynamicRouteEntity> getFilteredRoutes(@RequestParam(required = false) String routeId,
            @RequestParam(required = false) String description, @RequestParam(required = false) Integer version,
            @RequestParam(required = false) String path, @RequestParam(required = false) String httpMethod,
            @RequestParam(required = false) Boolean active, @RequestParam(required = false) Boolean defaultVersion,
            @RequestParam(required = false) String comment, @RequestParam(required = false) String yamlContains,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
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
        return routeService.getAllRoutes(spec, pageable);
    }
}
