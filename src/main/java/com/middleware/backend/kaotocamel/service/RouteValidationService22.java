package com.middleware.backend.kaotocamel.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import com.middleware.backend.kaotocamel.dto.RouteTestResult;
import com.middleware.backend.kaotocamel.dto.RouteValidationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteValidationService22 {

    private final CamelContext camelContext;
    private final RoutesBuilderLoader yamlRoutesLoader;

    public RouteValidationResult validateRoute(String yamlContent) {

        String tempRouteId = "test-" + UUID.randomUUID();
        // yamlContent = rewriteRouteIdForTesting(yamlContent, tempRouteId);

        Map<String, String> metadata = extractRouteMetadata(yamlContent);
        String routeId = metadata.get("id");
        String description = metadata.get("description");

        try {

            if (routeId == null || routeId.isBlank()) {
                return new RouteValidationResult(false, "Missing route ID in YAML.");
            }
            if (description == null || description.isBlank()) {
                return new RouteValidationResult(false, "Missing route description in YAML.");
            }

            loadRoute(yamlContent, tempRouteId);

            Optional<String> inputUri = detectInputUriByRouteId(routeId);
            String uri = inputUri.get();
            log.debug("\n\nExtracted Uri " + uri);
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
        String modifiedYaml = null;
        String tempRouteId = "test-route-" + UUID.randomUUID();
        // yamlContent = rewriteRouteIdForTesting(yamlContent, tempRouteId);

        String routeId = null;
        try {
            modifiedYaml = modifyYamlForTest(yamlContent);
            Map<String, String> metadata = extractRouteMetadata(modifiedYaml);
            routeId = metadata.get("id");
            log.debug("\n\n Extracted metadata: {}", metadata);
            log.debug("\n\n Modified YAML: {}", modifiedYaml);
            loadRoute(modifiedYaml, tempRouteId);
            // camelContext.start();
            camelContext.getRouteController().startRoute(routeId);
            camelContext.getRouteController().getRouteStatus(routeId);
            camelContext.getRoutes().forEach(route -> log.debug("Registered routeId: " + route.getId()));
            log.debug("routeId: " + routeId);
            log.debug("\n\n Route status: {}", camelContext.getRouteController().getRouteStatus(routeId));

            Optional<String> inputUri = detectInputUriByRouteId(routeId);
            if (inputUri.isEmpty()) { // no need
                return new RouteTestResult(false, null,
                        "No suitable input endpoint found (e.g. direct:*, seda:*, rest:*)");
            }

            String uri = inputUri.get();
            log.debug("\n\nExtracted Uri {}", uri);
            String response = camelContext.createProducerTemplate().requestBody("direct:testRoute",
                    testMessage != null ? testMessage : "Test Message", String.class);

            return new RouteTestResult(true, response, null);

        } catch (CamelExecutionException e) {
            log.error("Route test failed: {}", e.getMessage(), e);
            return new RouteTestResult(false, null, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to start test route: {}", e.getMessage(), e);
            return new RouteTestResult(false, null, e.getMessage());
        } finally {
            log.debug("\n\n Route status from final : {}", camelContext.getRouteController().getRouteStatus(routeId));
            if (routeId != null) {
                tryStopAndRemoveRoute(routeId);
            }
        }
    }

    /*
     * private void loadRoute(String yamlContent) { try { Resource resource = new
     * StringResource("inline:dynamic.yaml", yamlContent); RoutesBuilder builder =
     * yamlRoutesLoader.loadRoutesBuilder(resource);
     * camelContext.addRoutes(builder); } catch (Exception e) { throw new
     * RuntimeException("Failed to load route: " + e.getMessage(), e); } }
     */

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
                            route.put("id", idObj + "_test");
                        }

                        // Rewrite `from` URI if it's a REST route
                        Object fromObj = route.get("from");
                        if (fromObj instanceof Map) {
                            Map<String, Object> from = (Map<String, Object>) fromObj;
                            String uri = (String) from.get("uri");
                            if ("rest".equalsIgnoreCase(uri)) {
                                from.put("uri", "direct:testRoute");
                                from.remove("parameters");
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

    private Map<String, String> extractRouteMetadata11(String yaml) {
        boolean insideRoute = false;
        Map<String, String> metadata = new HashMap<>();

        for (String line : yaml.lines().toList()) {
            String trimmed = line.trim();

            if (trimmed.startsWith("- route:")) {
                insideRoute = true;
                continue;
            }

            if (insideRoute) {
                if (trimmed.startsWith("id:")) {
                    metadata.put("id", trimmed.split(":", 2)[1].trim());
                } else if (trimmed.startsWith("description:")) {
                    metadata.put("description", trimmed.split(":", 2)[1].trim());
                }

                // Exit if we hit a new top-level element
                if (!line.startsWith(" ") && !line.startsWith("\t")) {
                    break;
                }
            }
        }
        return metadata;
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

    public Optional<String> detectInputUri() {
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

    public String detectInputUri6(String id) {

        return camelContext.getRoute(id).getEndpoint().toString();
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

    public String rewriteRouteIdForTesting(String yamlContent, String newRouteId) {
        StringBuilder result = new StringBuilder();
        boolean insideRoute = false;

        for (String line : yamlContent.lines().toList()) {
            if (line.trim().startsWith("- route:")) {
                insideRoute = true;
            } else if (insideRoute && line.trim().startsWith("id:")) {
                // Replace the original route id with newRouteId
                int indent = line.indexOf("id:");
                String newLine = " ".repeat(indent) + "id: " + newRouteId;
                result.append(newLine).append("\n");
                continue;
            }
            result.append(line).append("\n");
        }
        return result.toString();
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
     * Helper class to wrap string as Resource for YAML loader
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
