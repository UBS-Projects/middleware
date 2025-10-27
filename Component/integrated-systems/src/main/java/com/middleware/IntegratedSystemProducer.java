package com.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.model.IntegratedSystemDetail;
import com.middleware.service.IntegratedSystemBridgeService;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer implementation for the IntegratedSystemEndpoint.
 * Fetches configuration details for a given system code and
 * populates the Camel Exchange message HEADER only (preserves body).
 */
public class IntegratedSystemProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(IntegratedSystemProducer.class);

    /**
     * The endpoint associated with this producer.
     */
    private final IntegratedSystemEndpoint endpoint;

    /**
     * Bridge service for fetching integrated system configuration.
     */
    private final IntegratedSystemBridgeService systemService;

    /**
     * Jackson ObjectMapper for serializing configuration data.
     */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructs a new IntegratedSystemProducer.
     *
     * @param endpoint      the endpoint
     * @param systemService the bridge service
     */
    public IntegratedSystemProducer(IntegratedSystemEndpoint endpoint, IntegratedSystemBridgeService systemService) {
        super(endpoint);
        this.endpoint = endpoint;
        this.systemService = systemService;
    }

    /**
     * Processes the Camel Exchange to fetch system configuration.
     * The system code is determined from the endpoint or message header.
     * Configuration is placed ONLY in header 'config', body is preserved.
     *
     * @param exchange the Camel exchange
     * @throws Exception if an error occurs during processing
     */
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

        // Convert config map to JSON
        String jsonConfig = mapper.writeValueAsString(detail.getConfig());

        // حط الـ config في header بس، ما تغير الـ body!
        exchange.getIn().setHeader("config", jsonConfig);

        // الـ body يبقى زي ما هو (CSV)
        LOG.info("Integrated system config loaded for '{}' and set in header 'config'. Body preserved.", detail.getCode());
    }
}