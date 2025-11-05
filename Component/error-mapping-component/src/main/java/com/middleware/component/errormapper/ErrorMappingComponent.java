package com.middleware.component.errormapper;

import com.middleware.component.errormapper.service.ErrorMappingBridgeService;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Camel component for applying error mappings dynamically.
 */
@Component("errorMapping")
public class ErrorMappingComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorMappingComponent.class);

    private ErrorMappingBridgeService bridgeService;

    public ErrorMappingComponent() {}

    public ErrorMappingComponent(CamelContext context) {
        super(context);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        ErrorMappingEndpoint endpoint = new ErrorMappingEndpoint(uri, this);
        setProperties(endpoint, parameters);

        if (this.bridgeService == null) {
            this.bridgeService = getCamelContext().getRegistry()
                    .lookupByNameAndType(ErrorMappingBridgeService.BEAN_ID, ErrorMappingBridgeService.class);

            if (this.bridgeService == null) {
                throw new IllegalStateException("ErrorMappingBridgeService is required but not found in Camel registry.");
            }
        }

        endpoint.setBridgeService(bridgeService);
        LOG.info("Created ErrorMappingEndpoint for URI: {}", uri);
        return endpoint;
    }
}