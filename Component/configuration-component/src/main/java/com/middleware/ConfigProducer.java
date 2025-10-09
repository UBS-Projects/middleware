package com.middleware;

import com.middleware.model.ConfigDetail;
import com.middleware.service.ConfigService;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        ConfigDetail detail = exchange.getIn().getBody(ConfigDetail.class);
        if (detail == null) {
            detail = new ConfigDetail();
            detail.setCode(endpoint.getCode());
        }
        ConfigDetail result = configService.getConfig (detail.getCode());
        if (result == null) {
            String code = exchange.getIn().getHeader("code", String.class);
            String message = "No configuration found for code: " + (code != null ? code : "UNKNOWN");

            exchange.getMessage().setBody(message);
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            exchange.getMessage().setHeader("error", true);

            // ✅ Mark the exchange as handled
            exchange.setProperty(Exchange.ROUTE_STOP, true);
            exchange.setProperty(Exchange.EXCEPTION_HANDLED, true);

            return;
        }
        LOG.info("Config Result Loaded: {}", result.getConfigs());

        // Set the fetched config as the response body
        result.getConfigs().forEach((key, value) -> {
            exchange.getMessage().setHeader(result.getCode(), value);
        });
    }
}
