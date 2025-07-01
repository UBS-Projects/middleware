package com.middleware.backend.kaotocamel.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.middleware.backend.kaotocamel.spec.DynamicRouteSpecification;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesBuilderLoader;
import org.apache.camel.support.ResourceSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.middleware.backend.kaotocamel.model.DynamicRouteAudit;
import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;
import com.middleware.backend.kaotocamel.repository.DynamicRouteAuditRepository;
import com.middleware.backend.kaotocamel.repository.DynamicRouteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicRouteService {

    private final CamelContext camelContext;
    private final DynamicRouteRepository routeRepository;
    private final DynamicRouteAuditRepository auditRepository;
    private final RoutesBuilderLoader yamlRoutesLoader;

    @Transactional
    public String uploadRoute(String yamlContent, String comment) {
        Map<String, String> metaData = extractRouteMetadata(yamlContent);
        String routeId = metaData.get("id");
        String description = metaData.get("description");
        String path = metaData.get("path");
        String method = metaData.get("method");

        if (routeId == null) {
            throw new RuntimeException("Route ID not found in YAML");
        }

        List<DynamicRouteEntity> versions = routeRepository.findByRouteIdOrderByVersionDesc(routeId);
        int newVersion = versions.isEmpty() ? 1 : versions.get(0).getVersion() + 1;

        versions.forEach(v -> v.setActive(false));
        routeRepository.saveAll(versions);

        loadRoute(yamlContent);

        DynamicRouteEntity entity = new DynamicRouteEntity();
        entity.setRouteId(routeId);
        entity.setVersion(newVersion);
        entity.setYamlContent(yamlContent);
        entity.setActive(true);
        entity.setDefaultVersion(true);
        entity.setDescription(description);
        entity.setPath(path);
        entity.setHttpMethod(method);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setComment(comment);

        routeRepository.save(entity);
        audit(routeId, newVersion, "upload", "Uploaded new version with comment: " + comment);

        return "Route " + routeId + " uploaded as version " + newVersion;
    }

    @Transactional
    public String deactivateRoute(String routeId) {
        List<DynamicRouteEntity> activeRoutes = routeRepository.findByRouteIdAndActiveTrue(routeId);
        if (activeRoutes.isEmpty()) {
            return "No active route found for: " + routeId;
        }

        activeRoutes.forEach(r -> r.setActive(false));
        routeRepository.saveAll(activeRoutes);

        try {
            camelContext.getRouteController().stopRoute(routeId);
            camelContext.removeRoute(routeId);
            audit(routeId, activeRoutes.get(0).getVersion(), "deactivate", "Deactivated current version");
            return "Route " + routeId + " deactivated.";
        } catch (Exception e) {
            log.error("Error deactivating route {}: {}", routeId, e.getMessage());
            return "Error deactivating route: " + e.getMessage();
        }
    }

    @Transactional
    public String revertToVersion(String routeId, int version) {
        Optional<DynamicRouteEntity> targetOpt = routeRepository.findByRouteIdAndVersion(routeId, version);
        if (targetOpt.isEmpty()) {
            return "Version not found";
        }

        deactivateRoute(routeId);
        loadRoute(targetOpt.get().getYamlContent());

        List<DynamicRouteEntity> versions = routeRepository.findByRouteIdOrderByVersionDesc(routeId);
        versions.forEach(v -> {
            boolean isTargetVersion = v.getVersion() == version;
            v.setActive(isTargetVersion);
            v.setDefaultVersion(isTargetVersion);
        });
        routeRepository.saveAll(versions);

        audit(routeId, version, "revert", "Reverted to version " + version);
        return "Reverted to route " + routeId + " version " + version;
    }

    @Transactional
    public String stopRoute(String routeId) {
        try {
            camelContext.getRouteController().stopRoute(routeId);
            List<DynamicRouteEntity> activeRoutes = routeRepository.findByRouteIdAndActiveTrue(routeId);
            activeRoutes.forEach(r -> r.setActive(false));
            routeRepository.saveAll(activeRoutes);

            audit(routeId, activeRoutes.get(0).getVersion(), "stop", "Route stopped");
            return "Route stopped: " + routeId;
        } catch (Exception e) {
            log.error("Error stopping route {}: {}", routeId, e.getMessage());
            return "Error stopping route: " + e.getMessage();
        }
    }

    @Transactional
    public String startRoute(String routeId) {
        List<DynamicRouteEntity> versions = routeRepository.findByRouteIdOrderByVersionDesc(routeId);
        if (versions.isEmpty()) {
            return "No route found with ID: " + routeId;
        }

        DynamicRouteEntity latest = versions.get(0);
        try {
            loadRoute(latest.getYamlContent());
            camelContext.getRouteController().startRoute(routeId);

            latest.setActive(true);
            latest.setDefaultVersion(true);
            routeRepository.save(latest);

            audit(routeId, latest.getVersion(), "start", "Route started");
            return "Route started: " + routeId;
        } catch (Exception e) {
            log.error("Error starting route {}: {}", routeId, e.getMessage());
            return "Error starting route: " + e.getMessage();
        }
    }

    public Page<DynamicRouteEntity> getAllRoutes(Pageable pageable) {
        return routeRepository.findAll(pageable);
    }

    public Page<DynamicRouteEntity> getAllRoutes(Specification<DynamicRouteEntity> spec, Pageable pageable) {
        return routeRepository.findAll(spec, pageable);
    }

    public Page<DynamicRouteEntity> getActiveRoutes(Pageable pageable) {
        return routeRepository.findByActiveTrue(pageable);
    }

    public Page<DynamicRouteEntity> getRoutesByRouteId(String routeId, Pageable pageable) {
        return routeRepository.findByRouteId(routeId, pageable);
    }

    public Optional<DynamicRouteEntity> getSpecificVersion(String routeId, int version) {
        return routeRepository.findByRouteIdAndVersion(routeId, version);
    }

    public List<DynamicRouteEntity> listVersions(String routeId) {
        return routeRepository.findByRouteIdOrderByVersionDesc(routeId);
    }

    public Page<DynamicRouteEntity> getLatestRoutesOptimized(Pageable pageable) {
        List<DynamicRouteEntity> allRoutes = routeRepository.findAllOrderByCreatedAtDesc();

        Map<String, DynamicRouteEntity> latestRoutes = allRoutes.stream()
                .collect(Collectors.groupingBy(
                        DynamicRouteEntity::getRouteId,
                        Collectors.reducing(null, this::selectLatestRoute)
                ));

        List<DynamicRouteEntity> routesList = latestRoutes.values().stream()
                .filter(route -> route != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        return applyPagination(routesList, pageable);
    }

    public Page<DynamicRouteEntity> getLatestRoutesWithFiltersOptimized(
            String routeId, String description, String path, String httpMethod,
            Boolean active, String comment, String yamlContains,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            Pageable pageable) {

        Specification<DynamicRouteEntity> spec = buildSpecification(
                routeId, description, path, httpMethod, active, comment,
                yamlContains, createdAfter, createdBefore);

        List<DynamicRouteEntity> filteredRoutes = routeRepository.findAll(spec);

        Map<String, DynamicRouteEntity> latestRoutes = filteredRoutes.stream()
                .collect(Collectors.groupingBy(
                        DynamicRouteEntity::getRouteId,
                        Collectors.reducing(null, this::selectLatestRoute)
                ));

        List<DynamicRouteEntity> routesList = latestRoutes.values().stream()
                .filter(route -> route != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());

        return applyPagination(routesList, pageable);
    }

    private DynamicRouteEntity selectLatestRoute(DynamicRouteEntity a, DynamicRouteEntity b) {
        if (a == null) return b;
        if (b == null) return a;

        if (b.isActive() && !a.isActive()) return b;
        if (a.isActive() && !b.isActive()) return a;

        if (b.isDefaultVersion() && !a.isDefaultVersion()) return b;
        if (a.isDefaultVersion() && !b.isDefaultVersion()) return a;

        if (b.getVersion() > a.getVersion()) return b;
        if (a.getVersion() > b.getVersion()) return a;

        return b.getCreatedAt().isAfter(a.getCreatedAt()) ? b : a;
    }

    private Page<DynamicRouteEntity> applyPagination(List<DynamicRouteEntity> routes, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), routes.size());

        List<DynamicRouteEntity> paginatedList = start >= routes.size() ?
                new ArrayList<>() : routes.subList(start, end);

        return new PageImpl<>(paginatedList, pageable, routes.size());
    }

    private Specification<DynamicRouteEntity> buildSpecification(
            String routeId, String description, String path, String httpMethod,
            Boolean active, String comment, String yamlContains,
            LocalDateTime createdAfter, LocalDateTime createdBefore) {

        return Specification
                .where(DynamicRouteSpecification.routeIdContains(routeId))
                .and(DynamicRouteSpecification.descriptionContains(description))
                .and(DynamicRouteSpecification.pathContains(path))
                .and(DynamicRouteSpecification.httpMethodContains(httpMethod))
                .and(DynamicRouteSpecification.hasActiveStatus(active))
                .and(DynamicRouteSpecification.containsComment(comment))
                .and(DynamicRouteSpecification.containsInYaml(yamlContains))
                .and(DynamicRouteSpecification.createdAfter(createdAfter))
                .and(DynamicRouteSpecification.createdBefore(createdBefore));
    }

    private void loadRoute(String yaml) {
        try {
            Resource resource = new StringResource("inline:dynamic.yaml", yaml);
            RoutesBuilder routesBuilder = yamlRoutesLoader.loadRoutesBuilder(resource);
            camelContext.addRoutes(routesBuilder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load route: " + e.getMessage(), e);
        }
    }

    private Map<String, String> extractRouteMetadata(String yamlContent) {
        Map<String, String> metadata = new HashMap<>();
        try {
            Yaml yaml = new Yaml(new SafeConstructor(new org.yaml.snakeyaml.LoaderOptions()));
            List<Object> parsed = yaml.load(yamlContent);

            if (parsed == null || parsed.isEmpty()) {
                throw new IllegalArgumentException("YAML content is empty or invalid");
            }

            for (Object item : parsed) {
                if (item instanceof Map) {
                    Map<String, Object> routeWrapper = (Map<String, Object>) item;
                    Object routeObj = routeWrapper.get("route");
                    if (routeObj instanceof Map) {
                        Map<String, Object> route = (Map<String, Object>) routeObj;

                        if (route.containsKey("id"))
                            metadata.put("id", ((String) route.get("id")).trim());

                        if (route.containsKey("description"))
                            metadata.put("description", ((String) route.get("description")).trim());

                        Object fromObj = route.get("from");
                        if (fromObj instanceof Map) {
                            Map<String, Object> from = (Map<String, Object>) fromObj;

                            if (from.containsKey("uri")) {
                                String uri = from.get("uri").toString();
                                if (uri.startsWith("rest:")) {
                                    String[] parts = uri.split(":", 3);
                                    if (parts.length == 3) {
                                        metadata.put("method", parts[1].trim());
                                        metadata.put("path", parts[2].trim());
                                    }
                                }
                            }

                            if (from.containsKey("parameters")) {
                                Map<String, Object> params = (Map<String, Object>) from.get("parameters");
                                if (params.containsKey("method")) {
                                    metadata.put("method", params.get("method").toString().trim());
                                }
                                if (params.containsKey("path")) {
                                    metadata.put("path", params.get("path").toString().trim());
                                }
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract route metadata from YAML", e);
        }
        return metadata;
    }

    private void audit(String routeId, int version, String action, String details) {
        DynamicRouteAudit audit = new DynamicRouteAudit();
        audit.setRouteId(routeId);
        audit.setVersion(version);
        audit.setAction(action);
        audit.setDetails(details);
        audit.setTimestamp(LocalDateTime.now());

        auditRepository.save(audit);
    }


    static class StringResource extends ResourceSupport {
        private final String content;

        public StringResource(String location, String content) {
            super("yaml", location);
            this.content = content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean exists() {
            return true;
        }
    }







    public Page<DynamicRouteAudit> getLatestRoutesLogs(Pageable pageable) {
        return auditRepository.findAll(pageable);
    }
    public Page<DynamicRouteAudit> getLatestRoutesLogsWithFilters(
            Long id,
            String routeId,
            Integer version,
            String action,
            String details,
            LocalDateTime timestamp,
            Pageable pageable) {

        return auditRepository.findByFilters(id, routeId, version, action, details, timestamp, pageable);
    }
}