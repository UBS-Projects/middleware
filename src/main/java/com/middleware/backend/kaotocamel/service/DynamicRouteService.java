package com.middleware.backend.kaotocamel.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesBuilderLoader;
import org.apache.camel.support.ResourceSupport;
import org.springframework.data.domain.Page;
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

    /**
     * Uploads a new route version (or first version if new routeId)
     */
    @Transactional
    public String uploadRoute(String yamlContent, String comment) {
        Map<String, String> metaData = extractRouteMetadata(yamlContent);
        String routeId = metaData.get("id");
        String description = metaData.get("description");
        String path = metaData.get("path");
        String method = metaData.get("method");
        if (routeId == null) { // verivication method should be added instead
            throw new RuntimeException("Route ID not found in YAML");
        }

        List<DynamicRouteEntity> versions = routeRepository.findByRouteIdOrderByVersionDesc(routeId);
        log.info("found {} versions", versions);
        int newVersion = versions.isEmpty() ? 1 : versions.get(0).getVersion() + 1;

        // Deactivate existing
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

        log.info("Uploaded route {} version {}", routeId, newVersion);
        audit(routeId, newVersion, "upload", "Uploaded new version with comment: " + comment);

        return "Route " + routeId + " uploaded as version " + newVersion;
    }

    /**
     * Deactivates a route (soft delete) and stops in Camel
     */
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
            log.info("Deactivated route {}", routeId);
            audit(routeId, activeRoutes.get(0).getVersion(), "deactivate", "Deactivated current version");
            return "Route " + routeId + " deactivated.";
        } catch (Exception e) {
            log.error("Error deactivating route {}: {}", routeId, e.getMessage());
            return "Error deactivating route: " + e.getMessage();
        }
    }

    /**
     * Reverts to a previous version of a route
     */
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

        log.info("Reverted {} to version {}", routeId, version);
        audit(routeId, version, "revert", "Reverted to version " + version);

        return "Reverted to route " + routeId + " version " + version;
    }

    /**
     * Stops a route
     */
    @Transactional
    public String stopRoute(String routeId) {
        try {
            camelContext.getRouteController().stopRoute(routeId);
            List<DynamicRouteEntity> activeRoutes = routeRepository.findByRouteIdAndActiveTrue(routeId);
            activeRoutes.forEach(r -> r.setActive(false));
            routeRepository.saveAll(activeRoutes);

            log.info("Stopped route {}", routeId);
            audit(routeId, activeRoutes.get(0).getVersion(), "stop", "Route stopped");
            return "Route stopped: " + routeId;
        } catch (Exception e) {
            log.error("Error stopping route {}: {}", routeId, e.getMessage());
            return "Error stopping route: " + e.getMessage();
        }
    }

    /**
     * Starts the latest version of a route
     */
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

            log.info("Started route {}", routeId);
            audit(routeId, latest.getVersion(), "start", "Route started");
            return "Route started: " + routeId;
        } catch (Exception e) {
            log.error("Error starting route {}: {}", routeId, e.getMessage());
            return "Error starting route: " + e.getMessage();
        }
    }

    /**
     * Lists all route
     */
    public Page<DynamicRouteEntity> getAllRoutes(Pageable pageable) {
        log.info("Fetching all routes with pagination: page={}, size={}", pageable.getPageNumber(),
                pageable.getPageSize());
        return routeRepository.findAll(pageable);
    }

    public Page<DynamicRouteEntity> getAllRoutes(Specification<DynamicRouteEntity> spec, Pageable pageable) {
        log.info("Fetching all routes with pagination: page={}, size={}", pageable.getPageNumber(),
                pageable.getPageSize());
        return routeRepository.findAll(spec, pageable);
    }

    public Page<DynamicRouteEntity> getActiveRoutes(Pageable pageable) {
        log.info("Fetching active routes with pagination");
        return routeRepository.findByActiveTrue(pageable);
    }

    @Transactional
    public Page<DynamicRouteEntity> getRoutesByRouteId(String routeId, Pageable pageable) {
        log.info("Fetching routes for routeId={} with pagination", routeId);
        return routeRepository.findByRouteId(routeId, pageable);
    }

    /**
     * Lists all versions of a route
     */
    @Transactional
    public List<DynamicRouteEntity> listVersions(String routeId) {
        return routeRepository.findByRouteIdOrderByVersionDesc(routeId);
    }

    /**
     * Loads a route YAML into Camel context
     */
    private void loadRoute(String yaml) {
        try {
            // RoutesLoader loader =
            // camelContext.getCamelContextExtension().getContextPlugin(RoutesLoader.class);
            // RoutesBuilder builder = loader.loadRoutesSource(new
            // ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), null);
            Resource resource = new StringResource("inline:dynamic.yaml", yaml);
            RoutesBuilder routesBuilder = yamlRoutesLoader.loadRoutesBuilder(resource);
            camelContext.addRoutes(routesBuilder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load route: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts routeId from YAML (simple parser)
     */
    /*
     * private String extractRouteId(String yaml) { return yaml.lines().filter(line
     * -> line.trim().startsWith("routeId:")).map(line -> line.split(":")[1].trim())
     * .findFirst().orElse(null); }
     */

    private String extractRouteId(String yaml) {
        boolean insideRoute = false;
        for (String line : yaml.lines().toList()) {
            String trimmed = line.trim();

            // Detect start of route block
            if (trimmed.startsWith("- route:")) {
                insideRoute = true;
                continue;
            }

            // If inside route, look for id
            if (insideRoute) {
                if (trimmed.startsWith("id:")) {
                    return trimmed.split(":", 2)[1].trim();
                }

                // If we reach a non-indented or unrelated line, exit
                if (!line.startsWith(" ") && !line.startsWith("\t")) {
                    break;
                }
            }
        }
        return null;
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

                        // Look inside `from`
                        Object fromObj = route.get("from");
                        if (fromObj instanceof Map) {
                            Map<String, Object> from = (Map<String, Object>) fromObj;

                            // 1. Try to extract from 'uri' like rest:post:/workflow/test
                            if (from.containsKey("uri")) {
                                String uri = from.get("uri").toString();
                                if (uri.startsWith("rest:")) {
                                    // e.g. rest:post:/workflow/test
                                    String[] parts = uri.split(":", 3);
                                    if (parts.length == 3) {
                                        metadata.put("method", parts[1].trim());
                                        metadata.put("path", parts[2].trim());
                                    }
                                }
                            }

                            // 2. Try to extract from 'parameters' map
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

    /**
     * Records an audit entry
     */
    private void audit(String routeId, int version, String action, String details) {
        DynamicRouteAudit audit = new DynamicRouteAudit();
        audit.setRouteId(routeId);
        audit.setVersion(version);
        audit.setAction(action);
        audit.setDetails(details);
        audit.setTimestamp(LocalDateTime.now());

        auditRepository.save(audit);
    }

    /**
     * Helper class to wrap a String as a Camel Resource.
     */
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
}
