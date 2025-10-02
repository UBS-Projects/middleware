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
        ConfigDetail result = configService.getConfigs(detail.getCode());
        LOG.info("Config Result Loaded: {}", result.getConfigs());

        // Set the fetched config as the response body
        result.getConfigs().forEach((key, value) -> {
            exchange.getMessage().setHeader(result.getCode() + "." + key, value);
        });
    }
}
