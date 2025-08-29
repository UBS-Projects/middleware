package com.middleware.backend.kaotocamel.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.middleware.backend.users.Roles.dto.RoutesPermissionsRequest;
import com.middleware.backend.users.Roles.service.RoutesPermissionsService;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.ServiceStatus;
import org.apache.camel.model.Model;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesBuilderLoader;
import org.apache.camel.support.ResourceSupport;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.middleware.backend.kaotocamel.spec.DynamicRouteSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
public class DynamicRouteService {

    private final CamelContext camelContext;
    private final DynamicRouteRepository routeRepository;
    private final DynamicRouteAuditRepository auditRepository;
    private final RoutesBuilderLoader yamlRoutesLoader;
    private final RestTemplateConfig restTemplateConfig;
    private final RoutesPermissionsService perService;

    /**
     * Uploads a new route version (or first version if new routeId)
     */
    @Transactional
    public String updateRoute(String yamlContent, String comment, String operation) {
        String routeId = null;
        Integer newVersion = null;
        String userEmail = "anonymous"; // fallback
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            userEmail = auth.getName(); // Usually the `sub` claim (email/username)
        }
        try {
            RouteValidationResult checkRouteMandatoryFields = checkRouteMandatoryFields(yamlContent);
            if (!checkRouteMandatoryFields.isValid()) {
                return checkRouteMandatoryFields.getErrorMessage();
            }

            Map<String, String> metaData = extractRouteMetadata(yamlContent);
            routeId = metaData.get("id");
            String description = metaData.get("description");
            String path = metaData.get("path");
            String method = metaData.get("method");

            // Check for duplicate route (path + method) when creating a new route
            if ("create".equalsIgnoreCase(operation) && path != null && method != null) {
                Optional<DynamicRouteEntity> existingRoute = routeRepository.findByPathAndHttpMethod(path, method);
                if (existingRoute.isPresent()) {
                    String duplicateRouteId = existingRoute.get().getRouteId();
                    String errorMsg = String.format("Duplicate route not allowed: path '%s' with method '%s' already exists in route '%s'", 
                                                    path, method, duplicateRouteId);
                    audit(routeId, -1, "create-failed", errorMsg, userEmail, "FAILED");
                    throw new RuntimeException(errorMsg);
                }
            }

            List<DynamicRouteEntity> versions = routeRepository.findByRouteIdOrderByVersionDesc(routeId);
            log.info("found {} versions", versions.size());

            if (versions.isEmpty() && "update".equalsIgnoreCase(operation)) {
                throw new RuntimeException("No versions found for this route");
            } else if (!versions.isEmpty() && "update".equalsIgnoreCase(operation)) {
                newVersion = versions.get(0).getVersion() + 1;
            } else if (!versions.isEmpty() && "create".equalsIgnoreCase(operation)) {
                throw new RuntimeException("Versions already exist for this route");
            } else if (versions.isEmpty() && "create".equalsIgnoreCase(operation)) {
                newVersion = 1;
            }

            // Deactivate existing versions
            versions.forEach(v -> {
                v.setActive(false);
                v.setDefaultVersion(false);
            });


            routeRepository.saveAll(versions);

            // Load into Camel Context
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


//            Save RouteId Into Permissions ...
            RoutesPermissionsRequest r = RoutesPermissionsRequest.builder()
                    .routeId(entity.getRouteId())
                    .build();
            perService.save(r);

            log.info("Uploaded route {} version {}", routeId, newVersion);
            audit(routeId, newVersion, "upload", comment==null? ""
                    :"Uploaded new version with comment: " + comment,userEmail
            ,"SUCCESS");

            return "Route " + routeId + " uploaded as version " + newVersion;

        } catch (Exception ex) {
            log.error("Failed to process route update for routeId={} version={} operation={} : {}",
                    routeId, newVersion, operation, ex.getMessage(), ex);
            audit(routeId, newVersion != null ? newVersion : -1, "error",
                    "Failed to " + operation + " route. Reason: " + ex.getMessage(),userEmail,
                    "FAILED");
            throw ex; // rethrow to trigger transaction rollback
        }
    }


    public RouteValidationResult validateRoute(String yamlContent) {
        String routeId = null;

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
            camelContext.getRouteController().startRoute(routeId);

            // HTTP-like schemes
            if ("rest".equals(uri) || "servlet".equals(uri) || "platform-http".equals(uri)) {
                String effectivePath = path;
                if ("servlet".equals(uri)) {
                    final String camelServletPrefix = "/camel";
                    if (!effectivePath.startsWith("/")) {
                        effectivePath = "/" + effectivePath;
                    }
                    effectivePath = camelServletPrefix + effectivePath;
                }
                String response = testRestRoute(effectivePath, method, testMessage != null ? testMessage : "Test Message", null);
                return new RouteTestResult(true, response, null);
            } else {
                // non-HTTP (direct/seda)
                Optional<String> inputUri = detectInputUriByRouteId(routeId);
                if (inputUri.isEmpty()) {
                    return new RouteTestResult(false, null, "No suitable input endpoint found (e.g. direct:*, seda:*, rest:*).");
                }
                String inUri = inputUri.get();
                String response = camelContext.createProducerTemplate().requestBody(
                        inUri,
                        testMessage != null ? testMessage : "Test Message",
                        String.class
                );
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

        // Replace path variables (e.g. {id}) with dummy test values
        String resolvedPath = path.replaceAll("\\{[^/]+\\}", "123");
        String uri = baseUrl + resolvedPath;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Add any additional headers if needed
        if (additionalHeaders != null) {
            additionalHeaders.forEach(headers::set);
        }

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, httpMethod, entity, String.class);
            return response.getBody();
        } catch (CamelExecutionException e) {
            throw e;
        }
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
                                from.put("uri", from.get("uri") + "!" + UUID.randomUUID());
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

            return routeDefinitions.stream()
                    .filter(r -> r.getInput() != null && r.getInput().getUri() != null)
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
        Integer version = null;
        String userEmail = "anonymous"; // fallback
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            userEmail = auth.getName(); // Usually the `sub` claim (email/username)
        }

        try {
            List<DynamicRouteEntity> activeRoutes = routeRepository.findByRouteIdAndActiveTrue(routeId);
            if (activeRoutes.isEmpty()) {
                audit(routeId, -1, "deactivate-failed",
                        "No active route found to deactivate.",userEmail ,
                        "FAILED");
                return "No active route found for: " + routeId;
            }

            // Deactivate in DB
            activeRoutes.forEach(r -> r.setActive(false));
            routeRepository.saveAll(activeRoutes);

            version = activeRoutes.get(0).getVersion();

            // Stop & remove from Camel
            camelContext.getRouteController().stopRoute(routeId);
            camelContext.removeRoute(routeId);

            log.info("Deactivated route {} version {}", routeId, version);
            audit(routeId, version, "deactivate", "Deactivated current version",userEmail,"SUCCESS");

            return "Route " + routeId + " deactivated.";

        } catch (Exception e) {
            log.error("Error deactivating route {} version={}: {}",
                    routeId, version, e.getMessage(), e);

            audit(routeId, version != null ? version : -1, "error",
                    "Failed to deactivate route. Reason: " + e.getMessage(),userEmail,
                    "FAILED");

            return ResponseEntity.badRequest().toString();
        }
    }


    /**
     * Reverts to a previous version of a route
     */
    @Transactional
    public String revertToVersion(String routeId, int version) {
        String userEmail = "anonymous"; // fallback
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            userEmail = auth.getName(); // Usually the `sub` claim (email/username)
        }
        try {
            Optional<DynamicRouteEntity> targetOpt = routeRepository.findByRouteIdAndVersion(routeId, version);
            if (targetOpt.isEmpty()) {
                audit(routeId, version, "revert-failed", "Target version not found",userEmail,
                        "FAILED");
                return "Version not found";
            }

            // Deactivate current route
            deactivateRoute(routeId);

            // Load the target YAML
            loadRoute(targetOpt.get().getYamlContent());

            // Update DB flags
            List<DynamicRouteEntity> versions = routeRepository.findByRouteIdOrderByVersionDesc(routeId);
            versions.forEach(v -> {
                boolean isTargetVersion = v.getVersion() == version;
                v.setActive(isTargetVersion);
                v.setDefaultVersion(isTargetVersion);
            });
            routeRepository.saveAll(versions);

            log.info("Reverted route {} to version {}", routeId, version);
            audit(routeId, version, "revert", "Reverted to version " + version,userEmail,"SUCCESS");

            return "Reverted to route " + routeId + " version " + version;

        } catch (Exception e) {
            log.error("Error reverting route {} to version {}: {}", routeId, version, e.getMessage(), e);
            audit(routeId, version, "error",
                    "Failed to revert to version. Reason: " + e.getMessage(),userEmail,
                    "FAILED");
            throw e; // ensure rollback
        }
    }


    @Transactional
    public String setVersionAsDefault(String routeId, int version) {
        String userEmail = "anonymous"; // fallback
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            userEmail = auth.getName(); // Usually the `sub` claim (email/username)
        }
        try {
            Optional<DynamicRouteEntity> targetOpt = routeRepository.findByRouteIdAndVersion(routeId, version);
            if (targetOpt.isEmpty()) {
                audit(routeId, version, "set-default-failed", "Target version not found",userEmail,
                        "FAILED");
                return "Version not found";
            }

            // Deactivate current route
            deactivateRoute(routeId);

            // Load the selected version
            loadRoute(targetOpt.get().getYamlContent());

            // Update DB flags
            List<DynamicRouteEntity> versions = routeRepository.findByRouteIdOrderByVersionDesc(routeId);
            versions.forEach(v -> {
                boolean isTargetVersion = v.getVersion() == version;
                v.setActive(isTargetVersion);
                v.setDefaultVersion(isTargetVersion);
            });
            routeRepository.saveAll(versions);

            log.info("Set route {} version {} as default", routeId, version);
            audit(routeId, version, "set-default", "Set version " + version + " as default",userEmail,"SUCCESS");

            return "Route " + routeId + " version " + version + " set as default.";

        } catch (Exception e) {
            log.error("Error setting route {} version {} as default: {}", routeId, version, e.getMessage(), e);
            audit(routeId, version, "error", "Failed to set version as default. Reason: " + e.getMessage(),userEmail,
                    "FAILED");
            throw e; // rollback if DB update fails
        }
    }


    /**
     * Stops a route
     */
    @Transactional
    public String stopRoute(String routeId) {
        Integer version = null;
        String userEmail = "anonymous"; // fallback
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            userEmail = auth.getName(); // Usually the `sub` claim (email/username)
        }

        try {
            // Stop Camel route
            camelContext.getRouteController().stopRoute(routeId);

            // Mark all active routes inactive in DB
            List<DynamicRouteEntity> activeRoutes = routeRepository.findByRouteIdAndActiveTrue(routeId);
            if (!activeRoutes.isEmpty()) {
                version = activeRoutes.get(0).getVersion();
                activeRoutes.forEach(r -> r.setActive(false));
                routeRepository.saveAll(activeRoutes);
            }

            log.info("Stopped route {} version {}", routeId, version);
            audit(routeId, version != null ? version : -1, "stop", "Route stopped",userEmail,"SUCCESS");

            return "Route stopped: " + routeId;

        } catch (Exception e) {
            log.error("Error stopping route {}: {}", routeId, e.getMessage(), e);
            audit(routeId, version != null ? version : -1, "error",
                    "Failed to stop route. Reason: " + e.getMessage(),userEmail,
                    "FAILED");
            return ResponseEntity.badRequest().toString();
        }
    }


    /**
     * Starts the latest version of a route
     */
    @Transactional
    public String startRoute(String routeId) {
        DynamicRouteEntity defaultVersion = routeRepository.findByRouteIdAndDefaultVersionTrue(routeId);
        String userEmail = "anonymous"; // fallback
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            userEmail = auth.getName(); // Usually the `sub` claim (email/username)
        }
        if (defaultVersion == null) {
            audit(routeId, -1, "start-failed", "No default route found with this ID",userEmail,
                    "FAILED");
            return "No default route found with ID: " + routeId;
        }

        try {
            // Load and start the Camel route
            loadRoute(defaultVersion.getYamlContent());
            camelContext.getRouteController().startRoute(routeId);

            // Mark version as active in DB
            defaultVersion.setActive(true);
            routeRepository.save(defaultVersion);

            log.info("Started route {} version {}", routeId, defaultVersion.getVersion());
            audit(routeId, defaultVersion.getVersion(), "start", "Route started",userEmail,"SUCCESS");

            return "Route started: " + routeId;

        } catch (Exception e) {
            log.error("Error starting route {} version={}: {}", routeId, defaultVersion.getVersion(), e.getMessage(), e);
            audit(routeId, defaultVersion.getVersion(), "error",
                    "Failed to start route. Reason: " + e.getMessage(),userEmail,
                    "FAILED");
            return ResponseEntity.badRequest().toString();
        }
    }


    /**
     * Lists all routes
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

    public Page<DynamicRouteEntity> getRoutesByRouteId(String routeId, Pageable pageable) {
        log.info("Fetching routes for routeId={} with pagination", routeId);
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

        Map<String, DynamicRouteEntity> latestRoutes = allRoutes.stream().collect(Collectors
                .groupingBy(DynamicRouteEntity::getRouteId, Collectors.reducing(null, this::selectLatestRoute)));

        List<DynamicRouteEntity> routesList = latestRoutes.values().stream()
                .filter(route -> route != null)
                .collect(Collectors.toList());

        // Apply sorting manually since we're working with a list
        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            boolean ascending = order.getDirection().isAscending();

            Comparator<DynamicRouteEntity> comparator = getComparator(property, ascending);
            routesList.sort(comparator);
        } else {
            // Default sort by createdAt descending
            routesList.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }

        return applyPagination(routesList, pageable);
    }

    private boolean applyAllFilters(DynamicRouteEntity route, String routeId, String description, String path,
                                    String httpMethod, Boolean active, String comment, String yamlContains,
                                    LocalDateTime createdAfter, LocalDateTime createdBefore) {

        // Apply active filter - THIS IS THE KEY FIX
        // If searching for inactive routes, only show routes where the LATEST version is inactive
        if (active != null && route.isActive() != active) {
            return false;
        }

        // Apply routeId filter
        if (routeId != null && !routeId.trim().isEmpty()) {
            if (!route.getRouteId().toLowerCase().contains(routeId.toLowerCase().trim())) {
                return false;
            }
        }

        // Apply description filter
        if (description != null && !description.trim().isEmpty()) {
            if (route.getDescription() == null ||
                    !route.getDescription().toLowerCase().contains(description.toLowerCase().trim())) {
                return false;
            }
        }

        // Apply path filter
        if (path != null && !path.trim().isEmpty()) {
            if (route.getPath() == null ||
                    !route.getPath().toLowerCase().contains(path.toLowerCase().trim())) {
                return false;
            }
        }

        // Apply httpMethod filter
        if (httpMethod != null && !httpMethod.trim().isEmpty()) {
            if (route.getHttpMethod() == null ||
                    !route.getHttpMethod().toLowerCase().contains(httpMethod.toLowerCase().trim())) {
                return false;
            }
        }

        // Apply comment filter
        if (comment != null && !comment.trim().isEmpty()) {
            if (route.getComment() == null ||
                    !route.getComment().toLowerCase().contains(comment.toLowerCase().trim())) {
                return false;
            }
        }

        // Apply yamlContains filter
        if (yamlContains != null && !yamlContains.trim().isEmpty()) {
            if (route.getYamlContent() == null ||
                    !route.getYamlContent().toLowerCase().contains(yamlContains.toLowerCase().trim())) {
                return false;
            }
        }

        // Apply date filters
        if (createdAfter != null && route.getCreatedAt().isBefore(createdAfter)) {
            return false;
        }

        if (createdBefore != null && route.getCreatedAt().isAfter(createdBefore)) {
            return false;
        }

        return true;
    }

    public Page<DynamicRouteEntity> getLatestRoutesWithFiltersOptimized(String routeId, String description, String path,
                                                                        String httpMethod, Boolean active, String comment, String yamlContains, LocalDateTime createdAfter,
                                                                        LocalDateTime createdBefore, Pageable pageable) {

        // Step 1: Get all routes and find the latest version of each routeId
        List<DynamicRouteEntity> allRoutes = routeRepository.findAllOrderByCreatedAtDesc();
        Map<String, DynamicRouteEntity> latestRoutesMap = allRoutes.stream().collect(Collectors
                .groupingBy(DynamicRouteEntity::getRouteId, Collectors.reducing(null, this::selectLatestRoute)));

        // Step 2: Convert to list and apply all filters
        List<DynamicRouteEntity> filteredRoutes = latestRoutesMap.values().stream()
                .filter(route -> route != null)
                .filter(route -> applyAllFilters(route, routeId, description, path, httpMethod, active,
                        comment, yamlContains, createdAfter, createdBefore))
                .collect(Collectors.toList());

        // Step 3: Apply sorting
        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            boolean ascending = order.getDirection().isAscending();

            Comparator<DynamicRouteEntity> comparator = getComparator(property, ascending);
            filteredRoutes.sort(comparator);
        } else {
            // Default sort by createdAt descending
            filteredRoutes.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }

        return applyPagination(filteredRoutes, pageable);
    }

    /**
     * Selects the latest/most appropriate route from two routes with the same routeId
     */
    private DynamicRouteEntity selectLatestRoute(DynamicRouteEntity a, DynamicRouteEntity b) {
        if (a == null) return b;
        if (b == null) return a;

        // Priority 1: Default version has highest priority (this is the "current" version)
        if (b.isDefaultVersion() && !a.isDefaultVersion()) return b;
        if (a.isDefaultVersion() && !b.isDefaultVersion()) return a;

        // Priority 2: Active routes have priority over inactive ones (among non-default versions)
        if (b.isActive() && !a.isActive()) return b;
        if (a.isActive() && !b.isActive()) return a;

        // Priority 3: Higher version number
        if (b.getVersion() > a.getVersion()) return b;
        if (a.getVersion() > b.getVersion()) return a;

        // Priority 4: More recent creation date
        return b.getCreatedAt().isAfter(a.getCreatedAt()) ? b : a;
    }

    /**
     * Creates a comparator for sorting routes by different properties
     */
    private Comparator<DynamicRouteEntity> getComparator(String property, boolean ascending) {
        Comparator<DynamicRouteEntity> comparator;

        switch (property) {
            case "routeId":
                comparator = Comparator.comparing(DynamicRouteEntity::getRouteId,
                        Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case "version":
                comparator = Comparator.comparing(DynamicRouteEntity::getVersion,
                        Comparator.nullsLast(Integer::compareTo));
                break;
            case "active":
                comparator = Comparator.comparing(DynamicRouteEntity::isActive,
                        Comparator.nullsLast(Boolean::compareTo));
                break;
            case "comment":
                comparator = Comparator.comparing(DynamicRouteEntity::getComment,
                        Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case "createdAt":
                comparator = Comparator.comparing(DynamicRouteEntity::getCreatedAt,
                        Comparator.nullsLast(LocalDateTime::compareTo));
                break;
            case "description":
                comparator = Comparator.comparing(DynamicRouteEntity::getDescription,
                        Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case "path":
                comparator = Comparator.comparing(DynamicRouteEntity::getPath,
                        Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case "httpMethod":
                comparator = Comparator.comparing(DynamicRouteEntity::getHttpMethod,
                        Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            default:
                comparator = Comparator.comparing(DynamicRouteEntity::getCreatedAt,
                        Comparator.nullsLast(LocalDateTime::compareTo));
                break;
        }

        return ascending ? comparator : comparator.reversed();
    }

    /**
     * Applies pagination to a list of routes
     */
    private Page<DynamicRouteEntity> applyPagination(List<DynamicRouteEntity> routes, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), routes.size());

        List<DynamicRouteEntity> paginatedList = start >= routes.size() ? new ArrayList<>()
                : routes.subList(start, end);

        return new PageImpl<>(paginatedList, pageable, routes.size());
    }

    /**
     * Builds JPA Specification for filtering routes
     */
    private Specification<DynamicRouteEntity> buildSpecification(String routeId, String description, String path,
                                                                 String httpMethod, Boolean active, String comment, String yamlContains, LocalDateTime createdAfter,
                                                                 LocalDateTime createdBefore) {

        return Specification.where(DynamicRouteSpecification.routeIdContains(routeId))
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
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            List<Object> parsed = yaml.load(yamlContent);

            if (parsed == null || parsed.isEmpty()) {
                throw new IllegalArgumentException("YAML content is empty or invalid.");
            }

            for (Object item : parsed) {
                if (!(item instanceof Map)) continue;

                Map<String, Object> routeWrapper = (Map<String, Object>) item;
                Object routeObj = routeWrapper.get("route");
                if (!(routeObj instanceof Map)) continue;

                Map<String, Object> route = (Map<String, Object>) routeObj;

                // id & description
                if (route.containsKey("id")) {
                    metadata.put("id", route.get("id").toString().trim());
                } else {
                    log.error("Route 'id' is missing.");
                }
                if (route.containsKey("description")) {
                    metadata.put("description", route.get("description").toString().trim());
                }

                // from
                Object fromObj = route.get("from");
                if (fromObj instanceof Map) {
                    Map<String, Object> from = (Map<String, Object>) fromObj;
                    String uri = from.get("uri") != null ? from.get("uri").toString().trim() : null;

                    Map<String, Object> params = null;
                    Object paramsObj = from.get("parameters");
                    if (paramsObj instanceof Map) {
                        params = (Map<String, Object>) paramsObj;
                    }

                    if (uri != null) {
                        // rest:METHOD:PATH
                        if (uri.startsWith("rest:")) {
                            String[] parts = uri.split(":", 3);
                            if (parts.length == 3) {
                                metadata.put("method", parts[1].trim());
                                metadata.put("path", parts[2].trim());
                                metadata.put("uri", "rest");
                            } else {
                                log.error("Invalid rest: URI format: " + uri);
                            }
                        }
                        // Kaoto rest مع parameters
                        else if (uri.equalsIgnoreCase("rest")) {
                            if (params != null) {
                                metadata.put("method", String.valueOf(params.getOrDefault("method", "")).trim());
                                metadata.put("path", String.valueOf(params.getOrDefault("path", "")).trim());
                                metadata.put("uri", "rest");
                            } else {
                                log.error("Missing parameters for rest uri.");
                            }
                        }
                        // servlet:/path
                        else if (uri.startsWith("servlet:/")) {
                            String path = uri.substring("servlet:".length());
                            metadata.put("path", path.trim());
                            String method = params != null ? String.valueOf(params.getOrDefault("httpMethodRestrict", "")).trim() : "";
                            if (method.contains(",")) method = method.split(",", 2)[0].trim();
                            metadata.put("method", method.isEmpty() ? "POST" : method);
                            metadata.put("uri", "servlet");
                        }
                        // platform-http:/path
                        else if (uri.startsWith("platform-http:/")) {
                            String path = uri.substring("platform-http:".length());
                            metadata.put("path", path.trim());
                            String method = params != null ? String.valueOf(params.getOrDefault("httpMethodRestrict", "")).trim() : "";
                            if (method.contains(",")) method = method.split(",", 2)[0].trim();
                            metadata.put("method", method.isEmpty() ? "POST" : method);
                            metadata.put("uri", "platform-http");
                        }
                        // direct/seda: فقط خزّن الـ uri (لا يوجد path/method)
                        else if (uri.startsWith("direct:") || uri.startsWith("seda:")) {
                            metadata.put("uri", uri);
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
    private void audit(String routeId, int version, String action, String details, String userEmail,String status) {
        DynamicRouteAudit audit = new DynamicRouteAudit();
        audit.setRouteId(routeId);
        audit.setVersion(version);
        audit.setAction(action);
        audit.setDetails(details);
        audit.setTimestamp(LocalDateTime.now());
        audit.setUserEmail(userEmail);
        audit.setStatus(status);

        auditRepository.save(audit);
    }

    public ResponseEntity<?> findById(Long id) {
        return ResponseEntity.ok(auditRepository.findById(id));
    }

    public byte[] exportFile(Specification<DynamicRouteAudit> spec, Pageable pageable, String type) {
        Page<DynamicRouteAudit> audits = auditRepository.findAll(spec, pageable);
        List<DynamicRouteAudit> data = audits.getContent();

        try {
            if ("CSV".equalsIgnoreCase(type)) {
                return exportToCsv(data);
            } else {
                return exporLogstToExcel(data);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export file: " + e.getMessage(), e);
        }
    }

    private byte[] exportToCsv(List<DynamicRouteAudit> audits) {
        StringBuilder sb = new StringBuilder();

        // Header row
        sb.append("ID,RouteId,Version,Action,Details,Timestamp,UserEmail,Status\n");

        // Data rows
        for (DynamicRouteAudit audit : audits) {
            sb.append(audit.getId()).append(",");
            sb.append(safe(audit.getRouteId())).append(",");
            sb.append(audit.getVersion()).append(",");
            sb.append(safe(audit.getAction())).append(",");
            sb.append(safe(audit.getDetails())).append(",");
            sb.append(audit.getTimestamp() != null ? audit.getTimestamp().toString() : "").append(",");
            sb.append(safe(audit.getUserEmail())).append(",");
            sb.append(safe(audit.getStatus())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] exporLogstToExcel(List<DynamicRouteAudit> audits) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("DynamicRouteLogs");

        // Header row
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "RouteId", "Version", "Action", "Details", "Timestamp", "UserEmail", "Status"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Data rows
        int rowIdx = 1;
        for (DynamicRouteAudit audit : audits) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(audit.getId());
            row.createCell(1).setCellValue(safe(audit.getRouteId()));
            row.createCell(2).setCellValue(audit.getVersion());
            row.createCell(3).setCellValue(safe(audit.getAction()));
            row.createCell(4).setCellValue(safe(audit.getDetails()));
            row.createCell(5).setCellValue(audit.getTimestamp() != null ? audit.getTimestamp().toString() : "");
            row.createCell(6).setCellValue(safe(audit.getUserEmail()));
            row.createCell(7).setCellValue(safe(audit.getStatus()));
        }

        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        workbook.write(baos);
        workbook.close();
        return baos.toByteArray();
    }

    private String safe(String value) {
        return value != null ? value.replace(",", " ") : "";
    }

    public String getRouteIdByPath(String path) {
        return routeRepository.findByPath(path).get().getRouteId();
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

    public Page<?> getLatestRoutesLogs(Specification<DynamicRouteAudit> spec, Pageable pageable) {
        return auditRepository.findAll(spec, pageable);
    }

    public Page<DynamicRouteAudit> getLatestRoutesLogsWithFilters(Long id, String routeId, Integer version,
                                                                  String action, String details, LocalDateTime timestamp, Pageable pageable) {
        return auditRepository.findByFilters(id, routeId, version, action, details, timestamp, pageable);
    }

    public byte[] exportToExcel(List<DynamicRouteEntity> routes) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Routes");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            // Create data style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Route ID", "Version", "Status", "Description", "Path", "HTTP Method", "Comment", "Created At"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int rowIndex = 1;

            for (DynamicRouteEntity route : routes) {
                Row row = sheet.createRow(rowIndex++);

                Cell cell0 = row.createCell(0);
                cell0.setCellValue(route.getRouteId() != null ? route.getRouteId() : "");
                cell0.setCellStyle(dataStyle);

                Cell cell1 = row.createCell(1);
                cell1.setCellValue(route.getVersion());
                cell1.setCellStyle(dataStyle);

                Cell cell2 = row.createCell(2);
                cell2.setCellValue(route.isActive() ? "ACTIVE" : "INACTIVE");
                cell2.setCellStyle(dataStyle);

                Cell cell3 = row.createCell(3);
                cell3.setCellValue(route.getDescription() != null ? route.getDescription() : "");
                cell3.setCellStyle(dataStyle);

                Cell cell4 = row.createCell(4);
                cell4.setCellValue(route.getPath() != null ? route.getPath() : "");
                cell4.setCellStyle(dataStyle);

                Cell cell5 = row.createCell(5);
                cell5.setCellValue(route.getHttpMethod() != null ? route.getHttpMethod() : "");
                cell5.setCellStyle(dataStyle);

                Cell cell6 = row.createCell(6);
                cell6.setCellValue(route.getComment() != null ? route.getComment() : "");
                cell6.setCellStyle(dataStyle);

                Cell cell7 = row.createCell(7);
                cell7.setCellValue(route.getCreatedAt() != null ? route.getCreatedAt().format(formatter) : "");
                cell7.setCellStyle(dataStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }
    }

    /**
     * Export routes to CSV format
     */
    public byte[] exportToCSV(List<DynamicRouteEntity> routes) throws IOException {
        StringBuilder csvBuilder = new StringBuilder();

        // Add header
        csvBuilder.append("Route ID,Version,Status,Description,Path,HTTP Method,Comment,Created At\n");

        // Add data rows
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (DynamicRouteEntity route : routes) {
            csvBuilder.append(escapeCsvValue(route.getRouteId())).append(",");
            csvBuilder.append(route.getVersion()).append(",");
            csvBuilder.append(route.isActive() ? "ACTIVE" : "INACTIVE").append(",");
            csvBuilder.append(escapeCsvValue(route.getDescription())).append(",");
            csvBuilder.append(escapeCsvValue(route.getPath())).append(",");
            csvBuilder.append(escapeCsvValue(route.getHttpMethod())).append(",");
            csvBuilder.append(escapeCsvValue(route.getComment())).append(",");
            csvBuilder.append(route.getCreatedAt() != null ? route.getCreatedAt().format(formatter) : "");
            csvBuilder.append("\n");
        }

        return csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Helper method to escape CSV values
     */
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }

        // If the value contains comma, quote, or newline, wrap it in quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            // Escape quotes by doubling them
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }

        return value;
    }


    @Scheduled(fixedRate = 30000)
    @Transactional
    public void syncRouteStatuses() {

        String userEmail = "System"; // fallback

        log.info("Starting route status sync with Camel context");

        List<DynamicRouteEntity> defaultRoutes = routeRepository.findAll();

        for (DynamicRouteEntity entity : defaultRoutes) {
            String routeId = entity.getRouteId();
            try {
                ServiceStatus status = camelContext.getRouteController().getRouteStatus(routeId);

                if (status == null) {
                    // Route not present in Camel context
                    if (entity.isActive()) {
                        // DB says active → re-add and start it
                        loadRoute(entity.getYamlContent()); // make sure this builds the route from entity
                        camelContext.getRouteController().startRoute(routeId);

                        audit(routeId, entity.getVersion(), "sync", "Re-added and started route in context",userEmail,"SUCCESS");
                        log.info("Re-added route {} to context (DB says active)", routeId);
                    } else {
                        // DB says inactive → nothing to do
                        log.info("Route {} not found in context and DB says inactive → skipping", routeId);
                    }
                    continue;
                }

                boolean isStarted = status.isStarted();

                // DB says active → ensure context route is running
                if (entity.isActive() && !isStarted) {
                    camelContext.getRouteController().startRoute(routeId);
                    audit(routeId, entity.getVersion(), "sync", "Started route in context (DB says active)",userEmail,"SUCCESS");
                    log.info("Started route {} in context (DB says active)", routeId);
                }

                // DB says inactive → ensure context route is stopped
                else if (!entity.isActive() && isStarted) {
                    camelContext.getRouteController().stopRoute(routeId);
                    audit(routeId, entity.getVersion(), "sync", "Stopped route in context (DB says inactive)",userEmail,"SUCCESS");
                    log.info("Stopped route {} in context (DB says inactive)", routeId);
                }

            } catch (Exception e) {
                log.error("Error syncing status for route {}: {}", routeId, e.getMessage(), e);
            }
        }

        log.info("Completed route status sync");
    }

}