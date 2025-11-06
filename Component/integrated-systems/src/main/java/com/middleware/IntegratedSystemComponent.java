package com.middleware;

import com.middleware.service.IntegratedSystemBridgeService;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Camel component for integrating with external systems.
 * It creates endpoints and manages the bridge service for system configurations.
 */
@Component("integratedSystem")
public class IntegratedSystemComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(IntegratedSystemComponent.class);

    /**
     * Bridge service for fetching integrated system configuration.
     */
    @Metadata(label = "internal")
    private IntegratedSystemBridgeService systemService;

    /**
     * Default constructor.
     */
    public IntegratedSystemComponent() {
    }

    /**
     * Constructor with Camel context.
     *
     * @param context the CamelContext
     */
    public IntegratedSystemComponent(CamelContext context) {
        super(context);
    }

    /**
     * Creates an endpoint for the integrated system component.
     *
     * @param uri        the endpoint URI
     * @param remaining  the remaining part of the URI
     * @param parameters endpoint parameters
     * @return the created Endpoint
     * @throws Exception if the system service is not found or an error occurs
     */
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        IntegratedSystemEndpoint endpoint = new IntegratedSystemEndpoint(uri, this);
        setProperties(endpoint, parameters);

        if (this.systemService == null) {
            this.systemService = getCamelContext().getRegistry()
                    .lookupByNameAndType("integratedSystemServiceBridge", IntegratedSystemBridgeService.class);

            if (this.systemService == null) {
                throw new IllegalStateException("IntegratedSystemService bridge is required but not found in Camel registry.");
            }
        }

        endpoint.setSystemService(systemService);
        LOG.info("Created IntegratedSystemEndpoint for URI: {}", uri);
        return endpoint;
    }
}