package com.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.model.IntegratedSystemDetail;
import com.middleware.service.IntegratedSystemBridgeService;
import com.middleware.service.IntegratedSystemDataSourceService;
import com.middleware.sql.IntegratedSystemSqlContext;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Producer for {@code integratedSystem}.
 * <ul>
 *   <li>Always loads config into header {@code config} (body preserved).</li>
 *   <li>HTTP/API systems: may add {@code Authorization} (Basic / Bearer).</li>
 *   <li>DATABASE systems: resolves a JDBC {@link DataSource} and binds it for
 *       subsequent Camel {@code sql} steps via {@link IntegratedSystemSqlContext}.</li>
 * </ul>
 */
public class IntegratedSystemProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(IntegratedSystemProducer.class);

    private final IntegratedSystemEndpoint endpoint;
    private final IntegratedSystemBridgeService systemService;
    private final ObjectMapper mapper = new ObjectMapper();

    public IntegratedSystemProducer(IntegratedSystemEndpoint endpoint, IntegratedSystemBridgeService systemService) {
        super(endpoint);
        this.endpoint = endpoint;
        this.systemService = systemService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String code = endpoint.getCode();

        if (code == null || code.isEmpty()) {
            code = exchange.getIn().getHeader("systemCode", String.class);
        }

        if (code == null || code.isEmpty()) {
            String message = "System code must be provided (via URI or header 'systemCode').";
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
            exchange.getIn().setHeader("error", true);
            LOG.warn(message);
            return;
        }

        IntegratedSystemDetail detail = systemService.getSystemConfig(code);

        if (detail == null) {
            String message = "No configuration found for integrated system code: " + code;
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            exchange.getIn().setHeader("error", true);
            LOG.warn(message);
            return;
        }

        Map<String, Object> configMap = detail.getConfig();
        String jsonConfig = mapper.writeValueAsString(configMap);
        exchange.getIn().setHeader("config", jsonConfig);

        if (isDatabaseSystem(configMap)) {
            bindDatabaseDataSource(exchange, code, detail);
            LOG.info(
                    "Integrated system config loaded for '{}' (DATABASE). DataSource bound for Camel SQL. Body preserved.",
                    detail.getCode());
            return;
        }

        applyHttpAuthorizationHeaders(detail.getCode(), configMap, exchange);
        LOG.info(
                "Integrated system config loaded for '{}' and set in header 'config'. Body preserved.",
                detail.getCode());
    }

    private void bindDatabaseDataSource(Exchange exchange, String code, IntegratedSystemDetail detail) {
        IntegratedSystemDataSourceService dataSourceService = lookupDataSourceService();
        if (dataSourceService == null) {
            throw new IllegalStateException(
                    "IntegratedSystemDataSourceService bean '"
                            + IntegratedSystemDataSourceService.BEAN_ID
                            + "' is required for DATABASE systems but was not found in the Camel registry.");
        }

        DataSource dataSource = dataSourceService.resolveDataSource(code, detail);
        IntegratedSystemSqlContext.bind(exchange, dataSource);
        LOG.info("JDBC DataSource resolved and bound for integrated system '{}'", code);
    }

    private IntegratedSystemDataSourceService lookupDataSourceService() {
        try {
            return getEndpoint()
                    .getCamelContext()
                    .getRegistry()
                    .lookupByNameAndType(
                            IntegratedSystemDataSourceService.BEAN_ID, IntegratedSystemDataSourceService.class);
        } catch (Exception e) {
            LOG.warn("Failed to lookup IntegratedSystemDataSourceService: {}", e.getMessage());
            return null;
        }
    }

    private static boolean isDatabaseSystem(Map<String, Object> configMap) {
        if (configMap == null) {
            return false;
        }
        Object systemType = configMap.get("systemType");
        return systemType != null && "DATABASE".equalsIgnoreCase(systemType.toString().trim());
    }

    /**
     * HTTP/API auth only — never add Authorization for DATABASE systems.
     */
    private static void applyHttpAuthorizationHeaders(String code, Map<String, Object> configMap, Exchange exchange) {
        Object authTypeObj = configMap.get("authenticationType");
        String authType = authTypeObj != null ? authTypeObj.toString() : null;

        if ("BASIC".equalsIgnoreCase(authType)) {
            String username = asString(configMap.get("username"));
            String password = asString(configMap.get("password"));

            if (username != null && password != null) {
                String auth = username + ":" + password;
                String encoded = java.util.Base64.getEncoder()
                        .encodeToString(auth.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                exchange.getIn().setHeader("Authorization", "Basic " + encoded);
                LOG.info("Basic Auth header added automatically for system '{}'", code);
            }
        }

        if ("TOKEN".equalsIgnoreCase(authType) || "JWT".equalsIgnoreCase(authType)) {
            String token = asString(configMap.get("token"));
            if (token != null) {
                exchange.getIn().setHeader("Authorization", "Bearer " + token);
                LOG.info("{} Auth header added automatically for system '{}'", authType, code);
            }
        }
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
