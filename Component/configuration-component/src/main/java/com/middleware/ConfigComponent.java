package com.middleware;

import java.util.Map;

import com.middleware.service.ConfigService;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("config")
public class ConfigComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigComponent.class);

    @Metadata(description = "The backend service used to retrieve configuration details.")
    private ConfigService configService;

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        ConfigEndpoint endpoint = new ConfigEndpoint(uri, this, remaining);
        setProperties(endpoint, parameters);

        if (this.configService == null) {
            this.configService = getCamelContext().getRegistry()
                    .lookupByNameAndType("configServiceBridge", ConfigService.class);

            if (this.configService == null) {
                throw new IllegalStateException("ConfigService bridge is required but not found in Camel registry.");
            }
        }

        endpoint.setConfigService(configService);
        LOG.info("Created ConfigEndpoint for URI: {}", uri);
        return endpoint;
    }

    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    public ConfigService getConfigService() {
        return configService;
    }
}
