package com.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.model.IntegratedSystemDetail;
import com.middleware.service.IntegratedSystemBridgeService;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            throw new IllegalArgumentException("System code must be provided (either via URI or header 'systemCode').");
        }

        IntegratedSystemDetail detail = systemService.getSystemConfig(code);
        if (detail == null) {
            throw new IllegalStateException("No configuration found for integrated system code: " + code);
        }
        System.out.println(detail);
        String jsonConfig = mapper.writeValueAsString(detail.getConfig());
        exchange.getMessage().setHeader(detail.getCode(), jsonConfig);
        System.out.println(jsonConfig);
        LOG.info("Integrated system config loaded for '{}': {}", detail.getCode(), jsonConfig);
    }
}
