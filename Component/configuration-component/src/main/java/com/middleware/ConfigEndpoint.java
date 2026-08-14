package com.middleware;

import com.middleware.service.ConfigService;
import org.apache.camel.Producer;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultEndpoint;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;
import org.apache.camel.Category;
import org.apache.camel.spi.Metadata;

/**
 * Apache Camel endpoint for configuration management operations.
 * <p>
 * This endpoint enables Camel routes to interact with configuration services. It is a
 * producer-only endpoint, meaning it can only send messages (retrieve configurations)
 * but cannot consume messages from a source.
 * </p>
 * <p>
 * The endpoint retrieves configuration data based on a configuration code and makes it
 * available to the Camel exchange as headers.
 * </p>
 *
 * <h3>URI Format</h3>
 * <pre>
 * config:operation?code=configCode
 * </pre>
 *
 * <h3>URI Options</h3>
 * <ul>
 *   <li><b>code</b> - The identifier for the configuration group to retrieve</li>
 * </ul>
 *
 * @author middleware
 * @version 1.0.0
 * @since 1.0.0
 * @see ConfigComponent
 * @see ConfigProducer
 */
@Getter
@Setter
@UriEndpoint(
        firstVersion = "1.0.0",
        scheme = "config",
        title = "Config",
        syntax = "config:operation",
        category = { Category.MESSAGING },
        producerOnly = true
)
public class ConfigEndpoint extends DefaultEndpoint {

    /**
     * Dummy path parameter required by Camel URI syntax.
     * <p>
     * This parameter is not used at runtime but is required by Camel's URI parsing.
     * </p>
     */
    @UriPath(description = "Dummy path required by Camel. Not used at runtime.")
    private String operation;

    /**
     * The configuration code identifying the configuration group.
     * <p>
     * This code is used to retrieve a specific set of configurations from the
     * configuration service.
     * </p>
     */
    @UriParam(description = "The code identifying the configuration group.")
    private String code;

    /**
     * The configuration service used to retrieve configuration data.
     * <p>
     * This service is injected by the {@link ConfigComponent} during endpoint creation.
     * </p>
     */
    private ConfigService configService;

    /**
     * Constructs a ConfigEndpoint with the specified URI and component.
     *
     * @param uri the endpoint URI
     * @param component the parent ConfigComponent
     */
    public ConfigEndpoint(String uri, ConfigComponent component) {
        super(uri, component);
    }

    /**
     * Creates a producer for this endpoint.
     * <p>
     * The producer is responsible for processing exchanges and retrieving configuration
     * data from the configuration service.
     * </p>
     *
     * @return a new ConfigProducer instance
     * @throws Exception if the producer cannot be created
     */
    @Override
    public Producer createProducer() throws Exception {
        return new ConfigProducer(this, configService);
    }

    /**
     * This endpoint does not support consumers.
     * <p>
     * The config component is producer-only and cannot consume messages.
     * </p>
     *
     * @param processor the processor (not used)
     * @return never returns, always throws exception
     * @throws UnsupportedOperationException always thrown as consumers are not supported
     */
    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("Config component is producer only.");
    }

    /**
     * Indicates whether this endpoint should be treated as a singleton.
     *
     * @return true, as this endpoint is singleton
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
}
