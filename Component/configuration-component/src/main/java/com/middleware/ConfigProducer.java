package com.middleware;

import com.middleware.model.ConfigDetail;
import com.middleware.service.ConfigService;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Producer implementation for the Config component.
 * <p>
 * Loads configuration for a given code and injects each key/value pair into
 * the exchange as Camel headers (body is preserved). Typical usage in a route:
 * </p>
 * <pre>
 *   .to("config:load?code=my.setting")
 *   // headers now contain the config key(s) for downstream steps
 * </pre>
 *
 * <h3>Error Handling</h3>
 * <p>
 * If the code is missing or no configuration is found, the producer sets HTTP 400/404,
 * an {@code error} header, and stops further route processing.
 * </p>
 *
 * @author middleware
 * @version 1.0.0
 * @since 1.0.0
 * @see ConfigEndpoint
 * @see ConfigComponent
 * @see ConfigService
 */
public class ConfigProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigProducer.class);

    private final ConfigEndpoint endpoint;
    private final ConfigService configService;

    public ConfigProducer(ConfigEndpoint endpoint, ConfigService configService) {
        super(endpoint);
        this.endpoint = endpoint;
        this.configService = configService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String code = resolveCode(exchange);

        if (code == null || code.isBlank()) {
            fail(exchange, 400, "Configuration code must be provided (via URI 'code' or body ConfigDetail).");
            return;
        }

        ConfigDetail result = configService.getConfig(code);
        if (result == null || result.getConfigs() == null || result.getConfigs().isEmpty()) {
            fail(exchange, 404, "No configuration found for code: " + code);
            return;
        }

        Map<String, String> configs = result.getConfigs();
        configs.forEach((key, value) -> exchange.getIn().setHeader(key, value));

        LOG.info("Config loaded for code='{}': injected {} header(s) {}", code, configs.size(), configs.keySet());
    }

    /**
     * Prefer body {@link ConfigDetail#getCode()}, then endpoint URI {@code code}.
     */
    private String resolveCode(Exchange exchange) {
        ConfigDetail detail = exchange.getIn().getBody(ConfigDetail.class);
        if (detail != null && detail.getCode() != null && !detail.getCode().isBlank()) {
            return detail.getCode().trim();
        }
        String endpointCode = endpoint.getCode();
        return endpointCode != null ? endpointCode.trim() : null;
    }

    private static void fail(Exchange exchange, int httpStatus, String message) {
        LOG.warn(message);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, httpStatus);
        exchange.getIn().setHeader("error", true);
        exchange.getIn().setBody(message);
        exchange.setProperty(Exchange.ROUTE_STOP, true);
        exchange.setProperty(Exchange.EXCEPTION_HANDLED, true);
    }
}
