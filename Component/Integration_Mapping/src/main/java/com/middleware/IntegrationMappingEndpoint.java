package com.middleware;

import com.middleware.service.IntegrationMappingService;
import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration Mapping endpoint.
 *
 * <p>Resolves a middleware mapping by name and delegates processing to the service layer.
 * Accepts a single parameter {@code mapping} (e.g. {@code healthmap}).</p>
 *
 * <p>Example:</p>
 * <pre>
 * integrationmapping?mapping=healthmap
 * </pre>
 */
@UriEndpoint(
        firstVersion = "1.0.0",
        scheme = "integrationmapping",
        title = "Integration Mapping",
        syntax = "integrationmapping",
        category = { Category.MESSAGING, Category.TRANSFORMATION },
        producerOnly = true
)
public class IntegrationMappingEndpoint extends DefaultEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrationMappingEndpoint.class);

    /**
     * Dummy path required by Camel. Not used at runtime.
     */
    @UriPath(description = "Dummy path required by Camel. Not used at runtime.")
    @Metadata(label = "internal")
    private String operation;

    /**
     * Middleware API mapping name (e.g., healthmap, education)
     */
    @UriParam(description = "Middleware API mapping name (e.g., healthmap, education)")
    private String mapping;

    @UriParam(defaultValue = "true", description = "Whether to validate parameters before processing")
    private boolean validateParams = true;

    @UriParam(defaultValue = "30000", description = "Connection timeout in milliseconds")
    private int connectionTimeout = 30000;

    @UriParam(defaultValue = "60000", description = "Read timeout in milliseconds")
    private int readTimeout = 60000;

    @Metadata(label = "internal")
    private IntegrationMappingService integrationMappingService;

    /**
     * Constructor - NO remaining parameter (like Notification component)
     */
    public IntegrationMappingEndpoint(String uri, IntegrationMappingComponent component) {
        super(uri, component);
        LOG.info("IntegrationMappingEndpoint created for URI: {}", uri);
    }

    @Override
    public Producer createProducer() throws Exception {
        LOG.info("Creating IntegrationMappingProducer for endpoint: {} with mapping: {}",
                getEndpointUri(), mapping);
        return new IntegrationMappingProducer(this, integrationMappingService);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException(
                "IntegrationMapping component is producer only.");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    // Getters and Setters
    public String getMapping() { return mapping; }
    public void setMapping(String mapping) { this.mapping = mapping; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public boolean isValidateParams() { return validateParams; }
    public void setValidateParams(boolean validateParams) { this.validateParams = validateParams; }

    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }

    // Internal setter only
    void setIntegrationMappingService(IntegrationMappingService integrationMappingService) {
        this.integrationMappingService = integrationMappingService;
    }
}