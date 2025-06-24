package com.middleware.backend.kaotocamel.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;
import com.middleware.backend.kaotocamel.service.DynamicRouteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
public class DynamicRouteController {

    private final DynamicRouteService routeService;

    @PostMapping("/upload")
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

    @GetMapping
    public Page<DynamicRouteEntity> getAllRoutes(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return routeService.getAllRoutes(PageRequest.of(page, size));
    }

    @GetMapping("/active")
    public Page<DynamicRouteEntity> getActiveRoutes(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return routeService.getActiveRoutes(PageRequest.of(page, size));
    }

    @GetMapping("/{routeId}")
    public Page<DynamicRouteEntity> getRoutesByRouteId(@PathVariable String routeId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return routeService.getRoutesByRouteId(routeId, PageRequest.of(page, size));
    }

    @GetMapping("/{routeId}/versions")
    public List<DynamicRouteEntity> versions(@PathVariable String routeId) {
        return routeService.listVersions(routeId);
    }

    @PostMapping("/{routeId}/stop")
    public String stop(@PathVariable String routeId) {
        return routeService.stopRoute(routeId);
    }

    @PostMapping("/{routeId}/start")
    public String start(@PathVariable String routeId) {
        return routeService.startRoute(routeId);
    }
}
