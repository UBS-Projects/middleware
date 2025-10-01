package com.middleware;

import com.middleware.model.ConfigDetail;
import com.middleware.service.ConfigService;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigProducer.class);

    private final ConfigService configService;
    private final String code;

    public ConfigProducer(ConfigEndpoint endpoint, ConfigService configService, String code) {
        super(endpoint);
        this.configService = configService;
        this.code = code;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        ConfigDetail detail = configService.getConfigs(code);
        if (detail != null) {
            LOG.info("Fetched config for code '{}': {}", code, detail.getConfigs());
            exchange.getMessage().setBody(detail.getConfigs());
        } else {
            LOG.warn("No config found for code '{}'", code);
            exchange.getMessage().setBody(null);
        }
    }
}
