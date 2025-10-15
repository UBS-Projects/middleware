package com.middleware;

import java.util.Map;

import com.middleware.service.ConfigService;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apache Camel component for configuration management.
 * <p>
 * This component provides integration with configuration services, allowing Camel routes
 * to retrieve and manage configuration data. It creates {@link ConfigEndpoint} instances
 * that can be used in Camel routes with the "config:" URI scheme.
 * </p>
 * <p>
 * The component requires a {@link ConfigService} bean named "configServiceBridge" to be
 * registered in the Camel registry.
 * </p>
 *
 * <h3>URI Format</h3>
 * <pre>
 * config:operation[?options]
 * </pre>
 *
 * @author middleware
 * @version 1.0.0
 * @since 1.0.0
 * @see ConfigEndpoint
 * @see ConfigProducer
 */
@Component("config")
public class ConfigComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigComponent.class);

    /**
     * The configuration service used to retrieve configuration data.
     * This is automatically looked up from the Camel registry.
     */
    @Metadata(label = "internal")
    private ConfigService configService;

    /**
     * Default constructor.
     * <p>
     * Creates a new ConfigComponent instance. The CamelContext will be set later.
     * </p>
     */
    public ConfigComponent() {
    }

    /**
     * Constructs a ConfigComponent with the specified CamelContext.
     *
     * @param context the CamelContext to be used by this component
     */
    public ConfigComponent(CamelContext context) {
        super(context);
    }

    /**
     * Creates a new endpoint for the given URI.
     * <p>
     * This method creates a {@link ConfigEndpoint} and automatically looks up the
     * {@link ConfigService} from the Camel registry if not already set. The service
     * must be registered with the name "configServiceBridge".
     * </p>
     *
     * @param uri the full URI of the endpoint
     * @param remaining the remaining part of the URI after the scheme
     * @param parameters the parameters specified in the URI
     * @return a new ConfigEndpoint instance
     * @throws Exception if the ConfigService cannot be found in the registry
     * @throws IllegalStateException if the ConfigService bridge is not found in the Camel registry
     */
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        ConfigEndpoint endpoint = new ConfigEndpoint(uri, this);
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
}
