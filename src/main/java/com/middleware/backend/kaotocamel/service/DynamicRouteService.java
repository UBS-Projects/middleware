package com.middleware.backend.kaotocamel.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.model.Model;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesBuilderLoader;
import org.apache.camel.support.ResourceSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.middleware.backend.config.RestTemplateConfig;
import com.middleware.backend.exception.InvalidRequestException;
import com.middleware.backend.kaotocamel.dto.RouteTestResult;
import com.middleware.backend.kaotocamel.dto.RouteValidationResult;
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
    @Autowired
    private final RestTemplateConfig restTemplateConfig;

    /**
     * Uploads a new route version (or first version if new routeId)
     */
    @Transactional
    public String uploadRoute(String yamlContent, String comment) {

        RouteValidationResult checkRouteMandatoryFields = checkRouteMandatoryFields(yamlContent);
        if (!checkRouteMandatoryFields.isValid()) {
            return checkRouteMandatoryFields.getErrorMessage();
        }

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
        versions.forEach(v -> {
            v.setActive(false);
            v.setDefaultVersion(false);
        });
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

    public RouteValidationResult validateRoute(String yamlContent) {

        // String tempRouteId = "test-" + UUID.randomUUID();
        // yamlContent = rewriteRouteIdForTesting(yamlContent, tempRouteId);

        // Map<String, String> metadata = extractRouteMetadata(yamlContent);
        String routeId = null;
        // String description = metadata.get("description");

        try {
            RouteValidationResult checkRouteMandatoryFields = checkRouteMandatoryFields(yamlContent);
            if (!checkRouteMandatoryFields.isValid()) {
                return checkRouteMandatoryFields;
            }
            yamlContent = modifyYamlForTest(yamlContent);
            Map<String, String> metadata = extractRouteMetadata(yamlContent);
            routeId = metadata.get("id");
            loadRoute(yamlContent);

            return new RouteValidationResult(true, null);
        } catch (Exception e) {
            log.error("Route validation failed: {}", e.getMessage(), e);
            return new RouteValidationResult(false, e.getMessage());
        } finally {
            if (routeId != null) {
                tryStopAndRemoveRoute(routeId);
            }
        }
    }

    public RouteTestResult testRoute(String yamlContent, String testMessage) {

        RouteValidationResult checkRouteMandatoryFields = checkRouteMandatoryFields(yamlContent);
        if (!checkRouteMandatoryFields.isValid()) {
            return new RouteTestResult(false, null, checkRouteMandatoryFields.getErrorMessage());
        }

        String modifiedYaml = null;
        // String tempRouteId = "test-route-" + UUID.randomUUID();
        // yamlContent = rewriteRouteIdForTesting(yamlContent, tempRouteId);

        String routeId = null;
        try {
            modifiedYaml = modifyYamlForTest(yamlContent);
            Map<String, String> metadata = extractRouteMetadata(modifiedYaml);
            routeId = metadata.get("id");
            String method = metadata.get("method");
            String path = metadata.get("path");
            String uri = metadata.get("uri");

            log.debug("\n\n Extracted metadata after modification: {}", metadata);
            log.debug("\n\n Modified YAML: {}", modifiedYaml);
            loadRoute(modifiedYaml);
            // camelContext.start();
            camelContext.getRouteController().startRoute(routeId);

            // camelContext.getRouteController().getRouteStatus(routeId);
            camelContext.getRoutes().forEach(route -> log.debug("Registered routeId: {} Status: {} \n", route.getId(),
                    camelContext.getRouteController().getRouteStatus(route.getId())));
            log.debug(" current routeId: " + routeId);
            // og.debug("\n\n Route status: {}",
            // camelContext.getRouteController().getRouteStatus(routeId));
            if ("rest".equals(uri)) {
                String response = testRestRoute(path, method, testMessage != null ? testMessage : "Test Message", null);
                return new RouteTestResult(true, response, null);
            } else {
                Optional<String> inputUri = detectInputUriByRouteId(routeId);
                if (inputUri.isEmpty()) { // no need
                    return new RouteTestResult(false, null,
                            "No suitable input endpoint found (e.g. direct:*, seda:*, rest:*)");
                }

                uri = inputUri.get();
                // log.debug("getEndpointRegistry {}\n getRuntimeEndpointRegistry {}\n
                // getRestConfiguration {}\n",
                // camelContext.getEndpointRegistry().toString(),
                // camelContext.getRuntimeEndpointRegistry(),
                // camelContext.getRestConfiguration());

                log.debug("\n\nExtracted Uri method detectInputUriByRouteId  {}", uri);
                String response = camelContext.createProducerTemplate().requestBody(uri,
                        testMessage != null ? testMessage : "Test Message", String.class);

                return new RouteTestResult(true, response, null);
            }

        } catch (CamelExecutionException e) {
            Throwable cause = e.getCause();
            String errorMsg = cause != null ? cause.getMessage() : e.getMessage();
            log.error("Route test failed: {}", errorMsg, e);
            return new RouteTestResult(false, null, "Execution failed: " + errorMsg);

        } catch (InvalidRequestException e) {
            log.error("Failed to start test route: {}", e.getMessage(), e);
            return new RouteTestResult(false, null, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to start test route: {}", e.getMessage(), e);
            return new RouteTestResult(false, null, e.getMessage());
        } finally {
            // log.debug("\n\n Route status from final : {}",
            // camelContext.getRouteController().getRouteStatus(routeId));
            if (routeId != null) {
                tryStopAndRemoveRoute(routeId);
            }
        }
    }

    public String testRestRoute(String path, String methodStr, String payload, Map<String, String> additionalHeaders)
            throws CamelExecutionException {
        RestTemplate restTemplate = new RestTemplate();

        HttpMethod httpMethod = HttpMethod.valueOf(methodStr.toUpperCase());
        String baseUrl = restTemplateConfig.getBaseUrl();
        // Construct the full URI
        String uri = baseUrl + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Add any additional headers if needed
        if (additionalHeaders != null) {
            additionalHeaders.forEach(headers::set);
        }
        Object bodyObj = (Object) payload;
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, httpMethod, entity, String.class);
            return response.getBody();
        } catch (CamelExecutionException e) {
            throw e;
        }

        // return response.getBody();
    }

    private RouteValidationResult checkRouteMandatoryFields(String yamlContent) {
        Map<String, String> metadata = extractRouteMetadata(yamlContent);
        String routeId = metadata.get("id");
        String description = metadata.get("description");

        if (routeId == null || routeId.isBlank()) {
            return new RouteValidationResult(false, "Missing route ID in YAML.");
        }
        if (description == null || description.isBlank()) {
            return new RouteValidationResult(false, "Missing route description in YAML.");
        } else
            return new RouteValidationResult(true, null);

    }

    private void loadRoute(String yamlContent, String routeId) {
        try {
            Resource resource = new StringResource("inline:" + routeId + ".yaml", yamlContent);
            RoutesBuilder builder = yamlRoutesLoader.loadRoutesBuilder(resource);
            camelContext.addRoutes(builder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load route: " + e.getMessage(), e);
        }
    }

    public static String modifyYamlForTest(String originalYaml) {
        try {
            // Parse the YAML
            Yaml yaml = new Yaml(new SafeConstructor(new org.yaml.snakeyaml.LoaderOptions()));
            List<Object> parsed = yaml.load(originalYaml);

            if (parsed == null || parsed.isEmpty()) {
                throw new IllegalArgumentException("YAML content is empty or invalid");
            }

            for (Object item : parsed) {
                if (item instanceof Map) {
                    Map<String, Object> routeWrapper = (Map<String, Object>) item;
                    Object routeObj = routeWrapper.get("route");

                    if (routeObj instanceof Map) {
                        Map<String, Object> route = (Map<String, Object>) routeObj;

                        // Update route ID
                        Object idObj = route.get("id");
                        if (idObj instanceof String) {
                            route.put("id", idObj + "_test-route");
                        }

                        // Rewrite `from` URI if it's a REST route
                        Object fromObj = route.get("from");
                        if (fromObj instanceof Map) {
                            Map<String, Object> from = (Map<String, Object>) fromObj;
                            String uri = (String) from.get("uri");
                            if ("rest:".equalsIgnoreCase(uri)) {
                                // from.put("uri", "direct:testRoute");
                                from.put("uri", from.get("uri") + "!" + UUID.randomUUID());
                                // from.remove("parameters");
                            } else if ("rest".equalsIgnoreCase(uri)) {
                                Object parmsObj = from.get("parameters");
                                if (parmsObj instanceof Map) {
                                    Map<String, Object> params = (Map<String, Object>) parmsObj;
                                    String path = (String) params.get("path");
                                    params.put("path", path + "!" + UUID.randomUUID());
                                }

                            }
                        }
                    }
                }
            }

            // Dump modified YAML
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml dumper = new Yaml(options);

            return dumper.dump(parsed);

        } catch (Exception e) {
            throw new RuntimeException("Failed to modify YAML for testing", e);
        }
    }

    public Optional<String> detectInputUri1() {
        try {
            // Cast camelContext to Model to access route definitions
            Model model = (Model) camelContext;
            List<RouteDefinition> routeDefinitions = model.getRouteDefinitions();

            return routeDefinitions.stream().filter(r -> r.getInput() != null && r.getInput().getUri() != null)// &&
                                                                                                               // id.equals(r.getInput().getId())
                    .map(r -> r.getInput().getUri())
                    .filter(uri -> uri.startsWith("direct:") || uri.startsWith("seda:") || uri.startsWith("rest:"))
                    .findFirst();
        } catch (Exception e) {
            log.warn("Failed to detect input URI: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    public Optional<String> detectInputUriByRouteId(String routeId) {
        try {
            var route = camelContext.getRoute(routeId);
            if (route != null && route.getEndpoint() != null) {
                String uri = route.getEndpoint().getEndpointUri();

                if (uri.startsWith("direct:") || uri.startsWith("seda:") || uri.startsWith("rest:")) {
                    return Optional.of(uri);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to detect input URI for routeId '{}': {}", routeId, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /*
     * public String rewriteRouteIdForTesting(String yamlContent, String newRouteId)
     * { StringBuilder result = new StringBuilder(); boolean insideRoute = false;
     * 
     * for (String line : yamlContent.lines().toList()) { if
     * (line.trim().startsWith("- route:")) { insideRoute = true; } else if
     * (insideRoute && line.trim().startsWith("id:")) { // Replace the original
     * route id with newRouteId int indent = line.indexOf("id:"); String newLine =
     * " ".repeat(indent) + "id: " + newRouteId;
     * result.append(newLine).append("\n"); continue; }
     * result.append(line).append("\n"); } return result.toString(); }
     */
    private void tryStopAndRemoveRoute(String routeId) {
        try {
            if (camelContext.getRouteController().getRouteStatus(routeId) != null) {
                camelContext.getRouteController().stopRoute(routeId);
                camelContext.removeRoute(routeId);
                log.info("Stopped and removed temporary route: {}", routeId);
            }
        } catch (Exception e) {
            log.warn("Cleanup failed for route {}: {}", routeId, e.getMessage());
        }
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

    @Transactional
    public String setVersionAsDefault(String routeId, int version) {
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
        // List<DynamicRouteEntity> versions =
        // routeRepository.findByRouteIdOrderByVersionDesc(routeId);
        DynamicRouteEntity defaultVersion = routeRepository.findByRouteIdAndDefaultVersionTrue(routeId);

        if ("null".equals(defaultVersion)) {
            return "No default route found with ID: " + routeId;
        }

        // DynamicRouteEntity latest = versions.get(0);
        try {
            loadRoute(defaultVersion.getYamlContent());
            camelContext.getRouteController().startRoute(routeId);

            defaultVersion.setActive(true);
            // latest.setDefaultVersion(true);
            routeRepository.save(defaultVersion);

            log.info("Started route {}", routeId);
            audit(routeId, defaultVersion.getVersion(), "start", "Route started");
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

    private Map<String, String> extractRouteMetadata(String yamlContent) {
        Map<String, String> metadata = new HashMap<>();
        try {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            List<Object> parsed = yaml.load(yamlContent);

            if (parsed == null || parsed.isEmpty()) {
                throw new IllegalArgumentException("YAML content is empty or invalid.");
            }

            for (Object item : parsed) {
                if (!(item instanceof Map)) {
                    continue;
                }

                Map<String, Object> routeWrapper = (Map<String, Object>) item;
                Object routeObj = routeWrapper.get("route");

                if (!(routeObj instanceof Map)) {
                    continue;
                }

                Map<String, Object> route = (Map<String, Object>) routeObj;

                // Route ID and description
                if (route.containsKey("id")) {
                    metadata.put("id", route.get("id").toString().trim());
                } else {
                    log.error("Route 'id' is missing.");
                }

                if (route.containsKey("description")) {
                    metadata.put("description", route.get("description").toString().trim());
                }

                // Extract from `from` section
                Object fromObj = route.get("from");
                if (fromObj instanceof Map) {
                    Map<String, Object> from = (Map<String, Object>) fromObj;

                    String uri = from.get("uri") != null ? from.get("uri").toString() : null;
                    if (uri != null) {
                        if (uri.startsWith("rest:")) {
                            String[] parts = uri.split(":", 3);
                            if (parts.length == 3) {
                                metadata.put("method", parts[1].trim());
                                metadata.put("path", parts[2].trim());
                                metadata.put("uri", "rest");
                            } else {
                                log.error("Invalid rest: URI format looking into parameters: " + uri);
                            }
                        } else if (uri.equalsIgnoreCase("rest")) {
                            // Handle Kaoto YAML style: method and path in parameters
                            Object paramsObj = from.get("parameters");
                            if (paramsObj instanceof Map) {
                                Map<String, Object> params = (Map<String, Object>) paramsObj;
                                metadata.put("method", String.valueOf(params.getOrDefault("method", "")).trim());
                                metadata.put("path", String.valueOf(params.getOrDefault("path", "")).trim());
                                metadata.put("uri", "rest");
                            }
                        } else if (uri.startsWith("direct:") || uri.startsWith("seda:")) {
                            metadata.put("uri", uri.trim());
                        } else {
                            log.error("Unrecognized 'uri' scheme: " + uri);
                        }
                    } else {
                        log.error("'uri' is missing in 'from' block.");
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
